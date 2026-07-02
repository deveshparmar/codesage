package com.deveshparmar.codesage.indexing.application;

import com.deveshparmar.codesage.common.domain.ChunkType;
import com.deveshparmar.codesage.common.util.HashUtils;
import com.deveshparmar.codesage.indexing.domain.CodeChunk;
import com.deveshparmar.codesage.indexing.domain.ParsedSourceFile;
import com.deveshparmar.codesage.indexing.domain.SourceChunkParser;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class JavaAstChunkParser implements SourceChunkParser {

    private static final String LANGUAGE = "java";

    @Override
    public boolean supports(String relativePath) {
        return relativePath.endsWith(".java");
    }

    @Override
    public ParsedSourceFile parse(
            String relativePath,
            String sourceCode,
            UUID repositoryId,
            String branchName,
            String commitSha
    ) {
        String contentHash = HashUtils.sha256(sourceCode);
        List<CodeChunk> chunks = new ArrayList<>();

        try {
            CompilationUnit compilationUnit = StaticJavaParser.parse(sourceCode);
            String packageName = compilationUnit.getPackageDeclaration()
                    .map(NodeWithName::getNameAsString)
                    .orElse("");

            for (ClassOrInterfaceDeclaration type : compilationUnit.findAll(ClassOrInterfaceDeclaration.class)) {
                if (type.isNestedType()) {
                    continue;
                }
                String className = type.getNameAsString();
                ChunkType typeChunk = type.isInterface() ? ChunkType.INTERFACE : ChunkType.CLASS;
                chunks.add(buildChunk(
                        typeChunk,
                        packageName,
                        className,
                        null,
                        type.getBegin().map(p -> p.line).orElse(1),
                        type.getEnd().map(p -> p.line).orElse(1),
                        type.toString(),
                        relativePath,
                        repositoryId,
                        branchName,
                        commitSha
                ));

                for (MethodDeclaration method : type.getMethods()) {
                    chunks.add(buildChunk(
                            ChunkType.METHOD,
                            packageName,
                            className,
                            method.getNameAsString(),
                            method.getBegin().map(p -> p.line).orElse(1),
                            method.getEnd().map(p -> p.line).orElse(1),
                            method.toString(),
                            relativePath,
                            repositoryId,
                            branchName,
                            commitSha
                    ));
                }

                for (ConstructorDeclaration constructor : type.getConstructors()) {
                    chunks.add(buildChunk(
                            ChunkType.CONSTRUCTOR,
                            packageName,
                            className,
                            constructor.getNameAsString(),
                            constructor.getBegin().map(p -> p.line).orElse(1),
                            constructor.getEnd().map(p -> p.line).orElse(1),
                            constructor.toString(),
                            relativePath,
                            repositoryId,
                            branchName,
                            commitSha
                    ));
                }
            }

            for (EnumDeclaration enumDeclaration : compilationUnit.findAll(EnumDeclaration.class)) {
                if (enumDeclaration.isNestedType()) {
                    continue;
                }
                String className = enumDeclaration.getNameAsString();
                chunks.add(buildChunk(
                        ChunkType.ENUM,
                        packageName,
                        className,
                        null,
                        enumDeclaration.getBegin().map(p -> p.line).orElse(1),
                        enumDeclaration.getEnd().map(p -> p.line).orElse(1),
                        enumDeclaration.toString(),
                        relativePath,
                        repositoryId,
                        branchName,
                        commitSha
                ));
            }

            for (RecordDeclaration recordDeclaration : compilationUnit.findAll(RecordDeclaration.class)) {
                if (recordDeclaration.isNestedType()) {
                    continue;
                }
                String className = recordDeclaration.getNameAsString();
                chunks.add(buildChunk(
                        ChunkType.RECORD,
                        packageName,
                        className,
                        null,
                        recordDeclaration.getBegin().map(p -> p.line).orElse(1),
                        recordDeclaration.getEnd().map(p -> p.line).orElse(1),
                        recordDeclaration.toString(),
                        relativePath,
                        repositoryId,
                        branchName,
                        commitSha
                ));
            }
        } catch (Exception ex) {
            chunks.clear();
        }

        return new ParsedSourceFile(
                java.nio.file.Path.of(relativePath),
                sourceCode,
                contentHash,
                LANGUAGE,
                chunks
        );
    }

    private CodeChunk buildChunk(
            ChunkType chunkType,
            String packageName,
            String className,
            String methodName,
            int startLine,
            int endLine,
            String content,
            String filePath,
            UUID repositoryId,
            String branchName,
            String commitSha
    ) {
        String chunkHash = HashUtils.sha256(
                filePath + "|" + chunkType + "|" + className + "|" + methodName + "|" + content
        );
        return CodeChunk.of(
                chunkType,
                chunkHash,
                packageName,
                className,
                methodName,
                startLine,
                endLine,
                content,
                repositoryId,
                branchName,
                commitSha,
                filePath,
                LANGUAGE
        );
    }
}
