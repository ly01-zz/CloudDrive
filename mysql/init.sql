-- ============================================================
-- 云盘数据库初始化脚本（docker mysql 首次启动自动执行）
-- 包含：8 张表结构 + 系统配置初始数据 + 初始管理员账号
-- 管理员账号：13800000000 / admin123（首次登录后请立即修改密码）
-- ============================================================

USE cloud_drive;

-- 用户表
CREATE TABLE IF NOT EXISTS `users` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `phone` varchar(20) NOT NULL COMMENT '手机号（唯一登录凭证）',
  `nickname` varchar(50) DEFAULT '' COMMENT '显示昵称',
  `email` varchar(100) DEFAULT NULL COMMENT '电子邮箱',
  `password_hash` varchar(255) NOT NULL COMMENT '密码哈希（bcrypt）',
  `avatar_url` varchar(500) DEFAULT NULL COMMENT '头像URL',
  `role` tinyint NOT NULL DEFAULT '0' COMMENT '角色：0-普通用户，1-管理员',
  `total_space` bigint NOT NULL DEFAULT '1073741824' COMMENT '总空间（字节），默认1GB',
  `used_space` bigint NOT NULL DEFAULT '0' COMMENT '已用空间（字节）',
  `monthly_download_limit` bigint NOT NULL DEFAULT '2147483648' COMMENT '每月下载流量（字节），默认2GB',
  `used_download_traffic` bigint NOT NULL DEFAULT '0' COMMENT '本月已用下载流量（字节）',
  `traffic_reset_time` datetime DEFAULT NULL COMMENT '流量统计月份锚点（用于判断跨月）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '账户状态：0-正常，1-冻结',
  `login_failed_count` tinyint NOT NULL DEFAULT '0' COMMENT '连续登录失败次数',
  `locked_until` datetime DEFAULT NULL COMMENT '账户锁定截止时间（NULL表示未锁定）',
  `last_login_ip` varchar(45) DEFAULT NULL COMMENT '最近登录IP（支持IPv6）',
  `last_login_time` datetime DEFAULT NULL COMMENT '最近登录时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '逻辑删除时间（NULL表示未删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_status_traffic` (`status`,`traffic_reset_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

-- 文件表
CREATE TABLE IF NOT EXISTS `files` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `user_id` bigint unsigned NOT NULL COMMENT '所属用户ID',
  `parent_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '父文件夹ID（0表示根目录）',
  `name` varchar(255) NOT NULL COMMENT '文件/文件夹名称',
  `is_folder` tinyint(1) NOT NULL DEFAULT '0' COMMENT '类型：0-文件，1-文件夹',
  `file_size` bigint NOT NULL DEFAULT '0' COMMENT '文件大小（字节），文件夹为0',
  `storage_path` varchar(500) DEFAULT NULL COMMENT 'COS存储路径（文件夹为NULL）',
  `file_sha256` varchar(64) DEFAULT NULL COMMENT '文件SHA-256（秒传）',
  `mime_type` varchar(100) DEFAULT NULL COMMENT '文件MIME类型',
  `download_count` int NOT NULL DEFAULT '0' COMMENT '文件下载次数',
  `upload_status` tinyint NOT NULL DEFAULT '1' COMMENT '上传状态：0-上传中，1-已完成',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '回收站标记（NULL表示未删除）',
  PRIMARY KEY (`id`),
  KEY `idx_user_parent` (`user_id`,`parent_id`,`deleted_at`),
  KEY `idx_user_md5` (`user_id`,`file_sha256`),
  KEY `idx_file_sha256` (`file_sha256`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户文件表';

-- 分享链接表
CREATE TABLE IF NOT EXISTS `share_links` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '创建者用户ID',
  `file_id` bigint NOT NULL COMMENT '被分享文件ID',
  `share_code` varchar(20) NOT NULL COMMENT '分享码（唯一标识）',
  `extract_code` varchar(10) DEFAULT NULL COMMENT '提取码（私密分享）',
  `share_type` tinyint NOT NULL DEFAULT '0' COMMENT '0-公开，1-私密',
  `access_mode` tinyint NOT NULL DEFAULT '0' COMMENT '0-仅下载（预留）',
  `total_visits` int DEFAULT '0' COMMENT '总访问次数',
  `max_visits` int DEFAULT NULL COMMENT '最大访问次数（null不限）',
  `total_downloads` int DEFAULT '0' COMMENT '累计下载次数',
  `max_downloads` int DEFAULT NULL COMMENT '最大下载次数（null不限）',
  `total_download_size` bigint DEFAULT '0' COMMENT '已消耗下载流量（字节）',
  `max_download_size` bigint DEFAULT NULL COMMENT '最大下载流量（字节，null不限）',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间（null永久）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0-正常，1-已过期，2-已取消',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_share_code` (`share_code`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_file_id` (`file_id`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分享链接表';

-- 下载日志表
CREATE TABLE IF NOT EXISTS `download_logs` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint unsigned NOT NULL COMMENT '下载用户ID',
  `file_id` bigint unsigned NOT NULL COMMENT '被下载文件ID',
  `download_size` bigint NOT NULL COMMENT '本次下载流量（字节）',
  `ip_address` varchar(45) DEFAULT NULL COMMENT '客户端IP',
  `user_agent` varchar(255) DEFAULT NULL COMMENT '客户端UA/设备信息',
  `share_code` varchar(20) DEFAULT NULL COMMENT '分享码（分享下载时记录）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下载时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='下载日志表';

-- 空间扩容申请表
CREATE TABLE IF NOT EXISTS `space_applications` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `user_id` bigint unsigned NOT NULL COMMENT '申请人ID',
  `apply_size` bigint NOT NULL COMMENT '申请增加的空间（字节）',
  `original_total` bigint NOT NULL COMMENT '申请前总空间（字节）',
  `reason` varchar(500) DEFAULT NULL COMMENT '申请原因',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0-待审批，1-已通过，2-已拒绝',
  `admin_id` bigint unsigned DEFAULT NULL COMMENT '审批管理员ID',
  `approve_remark` varchar(500) DEFAULT NULL COMMENT '审批备注',
  `apply_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='空间扩容申请表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS `system_config` (
  `config_key` varchar(50) NOT NULL COMMENT '配置键',
  `config_value` varchar(255) NOT NULL COMMENT '配置值',
  `description` varchar(200) DEFAULT NULL COMMENT '配置说明',
  PRIMARY KEY (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置表';

-- 管理员操作日志表
CREATE TABLE IF NOT EXISTS `admin_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_id` bigint NOT NULL COMMENT '操作管理员ID',
  `action` varchar(50) NOT NULL COMMENT '操作类型',
  `target_type` varchar(50) DEFAULT NULL COMMENT '操作对象类型',
  `target_id` varchar(50) DEFAULT NULL COMMENT '操作对象ID',
  `reason` varchar(255) DEFAULT NULL COMMENT '操作原因/备注',
  `ip_address` varchar(50) DEFAULT NULL COMMENT '操作IP',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员操作日志表';

-- 系统公告表
CREATE TABLE IF NOT EXISTS `announcements` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(100) NOT NULL COMMENT '公告标题',
  `content` text NOT NULL COMMENT '公告内容',
  `status` tinyint DEFAULT '0' COMMENT '0-发布中 1-已下架',
  `created_by` bigint DEFAULT NULL COMMENT '发布管理员ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统公告表';

-- ============================================================
-- 初始数据
-- ============================================================

-- 系统配置（注册逻辑依赖）
INSERT INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('default_space', '1073741824', '新用户默认空间大小（1GB，单位字节）'),
('max_user_limit', '100', '平台最大注册用户数'),
('monthly_traffic_limit', '2147483648', '每用户每月下载流量上限（2GB，单位字节）')
ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`);

-- 初始管理员账号（手机号 13800000000 / 密码 admin123，首次登录后请立即修改）
-- 密码哈希为 bcrypt("admin123")，如需修改密码：用项目登录页注册普通账号后在数据库中改 role=1，
-- 或运行 Spring 的 BCryptPasswordEncoder 生成新哈希替换
INSERT INTO `users` (`phone`, `nickname`, `password_hash`, `role`, `total_space`, `used_space`,
                     `monthly_download_limit`, `used_download_traffic`, `traffic_reset_time`, `status`,
                     `login_failed_count`, `locked_until`)
VALUES ('13800000000', '管理员', '$2a$10$vh8iDOV2kVg37VXQlO1P5eqZeVYaFuz.7dkAl9KEQ5SdH4ENygwk6',
        1, 107374182400, 0, 21474836480, 0, DATE_FORMAT(NOW(), '%Y-%m-01 00:00:00'), 0, 0, NULL)
ON DUPLICATE KEY UPDATE `phone` = `phone`;  -- 幂等：重复执行不报错
