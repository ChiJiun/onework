ALTER TABLE reservations
    ADD CONSTRAINT chk_review_state_consistency CHECK (
        (
            status = 'PROCESSING'
            AND reviewer_id IS NULL
            AND reviewed_at IS NULL
            AND rejection_reason IS NULL
        )
        OR (
            status = 'APPROVED'
            AND reviewer_id IS NOT NULL
            AND reviewed_at IS NOT NULL
            AND rejection_reason IS NULL
        )
        OR (
            status = 'REJECTED'
            AND reviewer_id IS NOT NULL
            AND reviewed_at IS NOT NULL
            AND rejection_reason IS NOT NULL
            AND btrim(rejection_reason) <> ''
        )
    );
