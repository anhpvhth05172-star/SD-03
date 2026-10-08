UPDATE dbo.phieu_giam_gia
SET ten_phieu_giam_gia = CASE
    WHEN UPPER(loai_giam_gia) = 'PERCENT'
        THEN N'Giảm ' + FORMAT(gia_tri_giam, '0.########', 'vi-VN') + N'%'
    ELSE N'Giảm ' + FORMAT(gia_tri_giam, '#,##0.########', 'vi-VN') + N'đ'
END;
