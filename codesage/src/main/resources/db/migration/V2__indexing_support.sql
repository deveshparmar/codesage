ALTER TABLE repositories
    ADD COLUMN scm_access_token TEXT;

CREATE INDEX idx_branches_repository ON branches (repository_id);
CREATE INDEX idx_files_branch ON files (branch_id);
