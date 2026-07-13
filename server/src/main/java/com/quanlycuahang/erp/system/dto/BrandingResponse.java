package com.quanlycuahang.erp.system.dto;

/**
 * Ten/khau hieu cua hang hien thi cong khai (Sidebar, trang dang nhap) — endpoint public, CHI duoc
 * chua 2 truong nay, khong duoc ghep them bat ky cau hinh nhay cam nao khac cua Settings.
 */
public record BrandingResponse(String storeName, String storeSlogan) {}
