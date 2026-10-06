-- Requires a verified backup and a quiesced write window.
-- User-approved policy: retain meetings and comments; hide deleted meetings.
ALTER TABLE meeting_records ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE meeting_records ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(50);
ALTER TABLE meeting_comments ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP;

-- Keep orphaned comment content and IDs intact, and classify it for recovery/auditing.
UPDATE meeting_comments SET archived_at = CURRENT_TIMESTAMP
WHERE archived_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM meeting_records m WHERE m.meeting_id = meeting_comments.meeting_id);
