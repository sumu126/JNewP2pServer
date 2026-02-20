-- P2P 信令服务器数据库表结构
-- 使用 JPA 自动建表时，此脚本可选
-- 如需手动建表，执行以下SQL

-- 文件信息表
CREATE TABLE IF NOT EXISTS `t_file_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `file_hash` VARCHAR(64) NOT NULL COMMENT '文件SHA-256哈希值',
    `file_name` VARCHAR(500) NOT NULL COMMENT '文件名',
    `file_size` BIGINT NOT NULL COMMENT '文件大小(字节)',
    `node_count` INT NOT NULL DEFAULT 0 COMMENT '拥有该文件的节点数量',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `idx_file_hash` (`file_hash`),
    KEY `idx_file_name` (`file_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件信息表';

-- 节点信息表
CREATE TABLE IF NOT EXISTS `t_node_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `node_id` VARCHAR(64) NOT NULL COMMENT '节点ID',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否活跃',
    `ip_address` VARCHAR(50) COMMENT '节点IP地址',
    `last_heartbeat` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后心跳时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `idx_node_id` (`node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点信息表';

-- 节点文件关联表
CREATE TABLE IF NOT EXISTS `t_node_file` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `node_id` VARCHAR(64) NOT NULL COMMENT '节点ID',
    `file_hash` VARCHAR(64) NOT NULL COMMENT '文件哈希值',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `idx_node_file` (`node_id`, `file_hash`),
    KEY `idx_node_id` (`node_id`),
    KEY `idx_file_hash` (`file_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点文件关联表';
