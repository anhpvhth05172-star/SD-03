-- Migration: them cot so_cccd (CCCD/CMND) cho bang khach_hang
-- Idempotent: chi chay khi cot chua ton tai

IF COL_LENGTH('khach_hang', 'so_cccd') IS NULL
    ALTER TABLE khach_hang ADD so_cccd NVARCHAR(20) NULL;
