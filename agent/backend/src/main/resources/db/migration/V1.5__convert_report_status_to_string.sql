UPDATE report SET status = 'GENERATING' WHERE status = '0';
UPDATE report SET status = 'GENERATED' WHERE status = '1';

