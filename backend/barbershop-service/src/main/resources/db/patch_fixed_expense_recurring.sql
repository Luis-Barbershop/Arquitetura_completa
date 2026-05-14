ALTER TABLE fixed_expenses
    ADD COLUMN recurring_monthly BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_fe_barbershop_recurring
    ON fixed_expenses (barbershop_id, recurring_monthly, year, month);
