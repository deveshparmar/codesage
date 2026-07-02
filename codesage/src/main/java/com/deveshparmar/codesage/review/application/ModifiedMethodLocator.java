package com.deveshparmar.codesage.review.application;

import com.deveshparmar.codesage.common.domain.ChunkType;
import com.deveshparmar.codesage.indexing.config.IndexingProperties;
import com.deveshparmar.codesage.review.domain.ModifiedMethod;
import com.deveshparmar.codesage.scm.domain.ScmPullRequestFile;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModifiedMethodLocator {

    private final UnifiedDiffParser diffParser;
    private final IndexingProperties indexingProperties;

    public List<ModifiedMethod> locateModifiedMethods(UUID repositoryId, List<ScmPullRequestFile> changedFiles) {
        List<ModifiedMethod> modifiedMethods = new ArrayList<>();
        for (ScmPullRequestFile file : changedFiles) {
            if (!file.filename().endsWith(".java")) {
                continue;
            }
            Set<Integer> changedLines = diffParser.parseChangedNewLines(file.patch());
            if (changedLines.isEmpty()) {
                continue;
            }
            modifiedMethods.addAll(locateInFile(repositoryId, file.filename(), file.patch(), changedLines));
        }
        return deduplicateMethods(modifiedMethods);
    }

    private List<ModifiedMethod> locateInFile(
            UUID repositoryId,
            String filePath,
            String patch,
            Set<Integer> changedLines
    ) {
        String sourceCode = readSourceFile(repositoryId, filePath);
        if (sourceCode == null || sourceCode.isBlank()) {
            return List.of(fallbackHunkReview(filePath, patch, changedLines));
        }

        try {
            CompilationUnit compilationUnit = StaticJavaParser.parse(sourceCode);
            List<ModifiedMethod> methods = new ArrayList<>();

            for (TypeDeclaration<?> type : compilationUnit.getTypes()) {
                collectTypeMethods(filePath, patch, changedLines, type, methods);
            }
            return methods.isEmpty() ? List.of(fallbackHunkReview(filePath, patch, changedLines)) : methods;
        } catch (Exception ex) {
            log.warn("Failed to parse Java file {}: {}", filePath, ex.getMessage());
            return List.of(fallbackHunkReview(filePath, patch, changedLines));
        }
    }

    private void collectTypeMethods(
            String filePath,
            String patch,
            Set<Integer> changedLines,
            TypeDeclaration<?> type,
            List<ModifiedMethod> methods
    ) {
        if (intersects(type, changedLines)) {
            ChunkType chunkType = resolveTypeChunk(type);
            methods.add(buildMethod(filePath, patch, type.getNameAsString(), null, chunkType, type));
        }

        if (type instanceof ClassOrInterfaceDeclaration classType) {
            for (MethodDeclaration method : classType.getMethods()) {
                if (intersects(method, changedLines)) {
                    methods.add(buildMethod(filePath, patch, classType.getNameAsString(), method.getNameAsString(), ChunkType.METHOD, method));
                }
            }
            for (ConstructorDeclaration constructor : classType.getConstructors()) {
                if (intersects(constructor, changedLines)) {
                    methods.add(buildMethod(filePath, patch, classType.getNameAsString(), constructor.getNameAsString(), ChunkType.CONSTRUCTOR, constructor));
                }
            }
        } else if (type instanceof EnumDeclaration enumType) {
            for (MethodDeclaration method : enumType.getMethods()) {
                if (intersects(method, changedLines)) {
                    methods.add(buildMethod(filePath, patch, enumType.getNameAsString(), method.getNameAsString(), ChunkType.METHOD, method));
                }
            }
        } else if (type instanceof RecordDeclaration recordType) {
            for (MethodDeclaration method : recordType.getMethods()) {
                if (intersects(method, changedLines)) {
                    methods.add(buildMethod(filePath, patch, recordType.getNameAsString(), method.getNameAsString(), ChunkType.METHOD, method));
                }
            }
        }
    }

    private ChunkType resolveTypeChunk(TypeDeclaration<?> type) {
        if (type.isClassOrInterfaceDeclaration()) {
            return type.asClassOrInterfaceDeclaration().isInterface() ? ChunkType.INTERFACE : ChunkType.CLASS;
        }
        if (type instanceof EnumDeclaration) {
            return ChunkType.ENUM;
        }
        if (type instanceof RecordDeclaration) {
            return ChunkType.RECORD;
        }
        return ChunkType.CLASS;
    }

    private ModifiedMethod buildMethod(
            String filePath,
            String patch,
            String className,
            String methodName,
            ChunkType chunkType,
            Node node
    ) {
        int startLine = node.getBegin().map(p -> p.line).orElse(1);
        int endLine = node.getEnd().map(p -> p.line).orElse(startLine);
        String source = node instanceof CallableDeclaration<?> callable ? callable.toString() : node.toString();
        return new ModifiedMethod(filePath, className, methodName, chunkType, startLine, endLine, source, patch);
    }

    private ModifiedMethod fallbackHunkReview(String filePath, String patch, Set<Integer> changedLines) {
        int minLine = changedLines.stream().min(Integer::compareTo).orElse(1);
        int maxLine = changedLines.stream().max(Integer::compareTo).orElse(minLine);
        return new ModifiedMethod(filePath, extractClassName(filePath), null, ChunkType.CLASS, minLine, maxLine, patch, patch);
    }

    private String extractClassName(String filePath) {
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        return fileName.endsWith(".java") ? fileName.substring(0, fileName.length() - 5) : fileName;
    }

    private boolean intersects(Node node, Set<Integer> changedLines) {
        int start = node.getBegin().map(p -> p.line).orElse(1);
        int end = node.getEnd().map(p -> p.line).orElse(start);
        for (int line = start; line <= end; line++) {
            if (changedLines.contains(line)) {
                return true;
            }
        }
        return false;
    }

    private String readSourceFile(UUID repositoryId, String filePath) {
        Path absolutePath = Path.of(indexingProperties.getWorkspacePath(), repositoryId.toString(), filePath);
        try {
            if (Files.exists(absolutePath)) {
                return Files.readString(absolutePath);
            }
        } catch (Exception ex) {
            log.warn("Unable to read source file {} from workspace", absolutePath);
        }
        return null;
    }

    private List<ModifiedMethod> deduplicateMethods(List<ModifiedMethod> methods) {
        Set<String> seen = new LinkedHashSet<>();
        List<ModifiedMethod> unique = new ArrayList<>();
        for (ModifiedMethod method : methods) {
            String key = method.filePath() + "|" + method.signature() + "|" + method.startLine() + "|" + method.endLine();
            if (seen.add(key)) {
                unique.add(method);
            }
        }
        return unique;
    }
}
