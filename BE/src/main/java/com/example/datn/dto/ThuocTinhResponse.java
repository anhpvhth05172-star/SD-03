package com.example.datn.dto;

import java.util.List;

public record ThuocTinhResponse(
    List<Item> loaiGiay,
    List<Item> thuongHieu,
    List<Item> chatLieu,
    List<Item> xuatXu,
    List<Item> kieuDang,
    List<Item> mauSac,
    List<Item> kichCo,
    List<Item> thanGiay,
    List<Item> deGiay
) {

    public record Item(Long id, String ma, String ten) {}
}
