WITH ranked_pings AS (
    SELECT
        id,
        ROW_NUMBER() OVER (PARTITION BY agent_id ORDER BY timestamp DESC) AS rn
    FROM agent_interaction
    WHERE type = 'PING'
)
DELETE FROM agent_interaction
WHERE id IN (
    SELECT id
    FROM ranked_pings
    WHERE rn > 30
);
