package com.bce.lunchies.repository;

import org.jooq.Field;
import org.jooq.Table;
import org.jooq.Record;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

public final class Tables {

    private Tables() {}

    /**
     * Extracts a LocalDate from a jOOQ Record, handling the R2DBC driver returning java.sql.Date.
     */
    public static LocalDate getLocalDate(org.jooq.Record record, Field<LocalDate> f) {
        Object val = record.get(f);
        if (val instanceof LocalDate ld) return ld;
        if (val instanceof java.sql.Date sd) return sd.toLocalDate();
        return null;
    }

    public static final class AppUser {
        public static final Table<Record> TABLE = table("app_user").asTable();
        public static final Field<UUID> ID = field("id", UUID.class);
        public static final Field<String> SLACK_USER_ID = field("slack_user_id", String.class);
        public static final Field<String> EMAIL = field("email", String.class);
        public static final Field<String> DISPLAY_NAME = field("display_name", String.class);
        public static final Field<String> ROLE = field("role", String.class);
        public static final Field<OffsetDateTime> CREATED_AT = field("created_at", OffsetDateTime.class);
        public static final Field<OffsetDateTime> LAST_LOGIN = field("last_login", OffsetDateTime.class);

        private AppUser() {}
    }

    public static final class Menu {
        public static final Table<Record> TABLE = table("menu").asTable();
        public static final Field<UUID> ID = field("id", UUID.class);
        public static final Field<LocalDate> MENU_DATE = field("menu_date", LocalDate.class);
        public static final Field<String> TITLE = field("title", String.class);
        public static final Field<UUID> CREATED_BY = field("created_by", UUID.class);
        public static final Field<String> SLACK_MESSAGE_TS = field("slack_message_ts", String.class);
        public static final Field<OffsetDateTime> CREATED_AT = field("created_at", OffsetDateTime.class);
        public static final Field<OffsetDateTime> UPDATED_AT = field("updated_at", OffsetDateTime.class);

        private Menu() {}
    }

    public static final class MenuItem {
        public static final Table<Record> TABLE = table("menu_item").asTable();
        public static final Field<UUID> ID = field("id", UUID.class);
        public static final Field<UUID> MENU_ID = field("menu_id", UUID.class);
        public static final Field<String> NAME = field("name", String.class);
        public static final Field<String> DESCRIPTION = field("description", String.class);
        public static final Field<Integer> SORT_ORDER = field("sort_order", Integer.class);
        public static final Field<String[]> TAGS = field("tags", String[].class);
        public static final Field<String[]> ALLERGENS = field("allergens", String[].class);

        private MenuItem() {}
    }

    public static final class Attendance {
        public static final Table<Record> TABLE = table("attendance").asTable();
        public static final Field<LocalDate> ATTENDANCE_DATE = field("attendance_date", LocalDate.class);
        public static final Field<UUID> USER_ID = field("user_id", UUID.class);
        public static final Field<Boolean> ATTENDING = field("attending", Boolean.class);
        public static final Field<OffsetDateTime> UPDATED_AT = field("updated_at", OffsetDateTime.class);

        private Attendance() {}
    }
}
