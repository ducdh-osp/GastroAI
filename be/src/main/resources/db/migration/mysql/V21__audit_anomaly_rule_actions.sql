-- Chuẩn hoá 1NF: audit_anomaly_rules.actions đang lưu NHIỀU giá trị trong 1 ô
-- ("PATIENT_LOCK,PATIENT_UNLOCK"). Tách thành bảng con, mỗi dòng 1 hành động mà quy tắc theo dõi.
-- Quy tắc không theo dõi hành động cụ thể nào (MULTI_IP - đếm theo IP) thì không có dòng con.
CREATE TABLE audit_anomaly_rule_actions (
    rule_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,

    PRIMARY KEY (rule_id, action),
    CONSTRAINT fk_audit_anomaly_rule_actions_rule
        FOREIGN KEY (rule_id) REFERENCES audit_anomaly_rules (id) ON DELETE CASCADE
);

-- Chép dữ liệu cũ: tách chuỗi theo dấu phẩy. Bảng số 1..10 = tối đa 10 hành động/quy tắc
-- (hiện nhiều nhất là 2). n chạy tới (số dấu phẩy + 1); SUBSTRING_INDEX lấy phần tử thứ n.
INSERT INTO audit_anomaly_rule_actions (rule_id, action)
SELECT DISTINCT r.id, TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(r.actions, ',', n.n), ',', -1))
FROM audit_anomaly_rules r
JOIN (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
      UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10) n
  ON n.n <= 1 + LENGTH(r.actions) - LENGTH(REPLACE(r.actions, ',', ''))
WHERE r.actions IS NOT NULL
  AND TRIM(r.actions) <> '';

ALTER TABLE audit_anomaly_rules DROP COLUMN actions;
