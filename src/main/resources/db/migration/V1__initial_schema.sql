-- Users
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE,
    name VARCHAR(100),
    phone VARCHAR(20),
    profile_image_url VARCHAR(500),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    login_provider VARCHAR(20),
    provider_id VARCHAR(255),
    token_version INT NOT NULL DEFAULT 0,
    last_login_date DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Points (Dual Currency - Primary)
CREATE TABLE points (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    total_amount INT NOT NULL DEFAULT 0,
    available_amount INT NOT NULL DEFAULT 0,
    today_earned_amount INT NOT NULL DEFAULT 0,
    today_used_amount INT NOT NULL DEFAULT 0,
    last_earned_date DATETIME,
    last_used_date DATETIME,
    last_reset_date DATETIME,
    last_access_date DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_points_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Point Transactions (Ledger - Source of Truth)
CREATE TABLE point_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount INT NOT NULL,
    transaction_type VARCHAR(10) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    source_id VARCHAR(100),
    description VARCHAR(500),
    expiration_date DATETIME,
    state INT NOT NULL DEFAULT 1,
    winning_history_id BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_pt_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_pt_user_expiration (user_id, expiration_date, state),
    INDEX idx_pt_user_type_state (user_id, transaction_type, state),
    INDEX idx_pt_earned_date (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tickets (Dual Currency - Secondary)
CREATE TABLE tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    total_amount INT NOT NULL DEFAULT 0,
    available_amount INT NOT NULL DEFAULT 0,
    today_earned_amount INT NOT NULL DEFAULT 0,
    today_used_amount INT NOT NULL DEFAULT 0,
    last_earned_date DATETIME,
    last_used_date DATETIME,
    last_reset_date DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_tickets_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Ticket Transactions (FIFO tracking)
CREATE TABLE ticket_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount INT NOT NULL,
    transaction_type VARCHAR(10) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    description VARCHAR(500),
    expiration_date DATETIME,
    remaining_amount INT NOT NULL DEFAULT 0,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    related_transaction_id BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_tt_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_tt_user_type (user_id, transaction_type),
    INDEX idx_tt_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Game Actions
CREATE TABLE actions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action_name VARCHAR(100) NOT NULL,
    action_type VARCHAR(30) NOT NULL,
    success_reward INT NOT NULL DEFAULT 1,
    fail_reward INT NOT NULL DEFAULT 0,
    daily_limit INT NOT NULL DEFAULT 5,
    max_daily_reward INT NOT NULL DEFAULT 50,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ord INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- User Action Logs
CREATE TABLE user_action_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    action_id BIGINT NOT NULL,
    success BOOLEAN NOT NULL,
    earned_reward INT NOT NULL DEFAULT 0,
    participation_date DATE NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_ual_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ual_action FOREIGN KEY (action_id) REFERENCES actions(id),
    INDEX idx_ual_user_action_date (user_id, action_id, participation_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Prizes
CREATE TABLE prizes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    brand VARCHAR(100),
    description TEXT,
    image_url VARCHAR(500),
    prize_type VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Random Boxes
CREATE TABLE random_boxes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    box_type VARCHAR(10) NOT NULL,
    ticket_cost INT NOT NULL,
    image_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    reentry_restriction_days INT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Random Box Prizes (Probability Configuration)
CREATE TABLE random_box_prizes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    random_box_id BIGINT NOT NULL,
    prize_id BIGINT NOT NULL,
    winning_probability DOUBLE NOT NULL,
    display_probability VARCHAR(50),
    winning_period_days INT,
    total_winning_count INT,
    remaining_count INT,
    winning_start_date DATETIME,
    min_point_value INT,
    max_point_value INT,
    social_proof_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    push_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_rbp_box FOREIGN KEY (random_box_id) REFERENCES random_boxes(id),
    CONSTRAINT fk_rbp_prize FOREIGN KEY (prize_id) REFERENCES prizes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Winning Histories
CREATE TABLE winning_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    random_box_id BIGINT NOT NULL,
    random_box_prize_id BIGINT,
    points_awarded INT NOT NULL DEFAULT 0,
    ticket_cost INT NOT NULL DEFAULT 0,
    prize_name VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_wh_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_wh_box FOREIGN KEY (random_box_id) REFERENCES random_boxes(id),
    INDEX idx_wh_user (user_id),
    INDEX idx_wh_box_prize (random_box_id, random_box_prize_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Ad Platforms
CREATE TABLE ad_platforms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    platform_name VARCHAR(50) NOT NULL,
    ad_type VARCHAR(20) NOT NULL,
    priority INT NOT NULL,
    ad_unit_id_android VARCHAR(200),
    ad_unit_id_ios VARCHAR(200),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    fill_rate DOUBLE NOT NULL DEFAULT 0.0,
    ctr DOUBLE NOT NULL DEFAULT 0.0,
    total_impressions BIGINT NOT NULL DEFAULT 0,
    total_clicks BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Invite Codes
CREATE TABLE invite_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(8) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_ic_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Invite Histories
CREATE TABLE invite_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inviter_id BIGINT NOT NULL,
    invitee_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_ih_inviter FOREIGN KEY (inviter_id) REFERENCES users(id),
    CONSTRAINT fk_ih_invitee FOREIGN KEY (invitee_id) REFERENCES users(id),
    INDEX idx_ih_inviter (inviter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Invite Rewards (Milestone Configuration)
CREATE TABLE invite_rewards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    milestone_sorting INT NOT NULL,
    ticket_reward INT NOT NULL DEFAULT 0,
    point_reward INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Invite Milestone Rewards (Achievement Tracking)
CREATE TABLE invite_milestone_rewards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    invite_reward_id BIGINT NOT NULL,
    achieved_date DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_imr_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_imr_reward FOREIGN KEY (invite_reward_id) REFERENCES invite_rewards(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Initial Data: Game Actions
INSERT INTO actions (action_name, action_type, success_reward, fail_reward, daily_limit, max_daily_reward, is_active, ord, created_at, updated_at) VALUES
('Daily Check-in', 'ATTENDANCE', 3, 0, 1, 3, TRUE, 1, NOW(), NOW()),
('Rock Paper Scissors', 'RPS', 2, 1, 10, 20, TRUE, 2, NOW(), NOW()),
('Picture Matching', 'PICTURE_MATCHING', 3, 1, 5, 15, TRUE, 3, NOW(), NOW()),
('Lucky Roulette', 'ROULETTE', 5, 0, 3, 15, TRUE, 4, NOW(), NOW()),
('Stopwatch Challenge', 'STOPWATCH', 2, 0, 5, 10, TRUE, 5, NOW(), NOW()),
('Find the Cat', 'FIND_CAT', 3, 1, 5, 15, TRUE, 6, NOW(), NOW()),
('Watch & Earn', 'AD_REWARDED', 3, 0, 10, 30, TRUE, 7, NOW(), NOW()),
('Omok Battle', 'OMOK', 5, 1, 3, 15, TRUE, 8, NOW(), NOW());

-- Initial Data: Ad Platforms
INSERT INTO ad_platforms (platform_name, ad_type, priority, is_active, fill_rate, created_at, updated_at) VALUES
('ADMOB', 'REWARDED', 1, TRUE, 85.5, NOW(), NOW()),
('UNITY_ADS', 'REWARDED', 2, TRUE, 78.2, NOW(), NOW()),
('VUNGLE', 'REWARDED', 3, TRUE, 72.1, NOW(), NOW());

-- Initial Data: Invite Milestones
INSERT INTO invite_rewards (milestone_sorting, ticket_reward, point_reward, created_at, updated_at) VALUES
(5, 50, 500, NOW(), NOW()),
(10, 100, 1000, NOW(), NOW()),
(15, 150, 1500, NOW(), NOW()),
(20, 200, 2000, NOW(), NOW());
