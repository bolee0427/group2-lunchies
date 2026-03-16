CREATE TABLE app_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slack_user_id VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255),
    display_name VARCHAR(255),
    role VARCHAR(20) NOT NULL DEFAULT 'USER' CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'USER')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login TIMESTAMPTZ
);

CREATE TABLE menu (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_date DATE NOT NULL UNIQUE,
    title VARCHAR(100),
    created_by UUID NOT NULL REFERENCES app_user(id),
    slack_message_ts VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE menu_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_id UUID NOT NULL REFERENCES menu(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(200),
    sort_order INT NOT NULL DEFAULT 0,
    tags VARCHAR[] DEFAULT '{}',
    allergens VARCHAR[] DEFAULT '{}',
    CONSTRAINT valid_sort CHECK (sort_order >= 0)
);

CREATE TABLE attendance (
    attendance_date DATE NOT NULL,
    user_id UUID NOT NULL REFERENCES app_user(id),
    attending BOOLEAN NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (attendance_date, user_id)
);
