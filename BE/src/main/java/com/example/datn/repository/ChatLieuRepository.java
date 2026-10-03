package com.example.datn.repository;

import com.example.datn.entity.ChatLieu;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatLieuRepository extends JpaRepository<ChatLieu, Long> {

    boolean existsByTenChatLieuIgnoreCase(String ten);

    boolean existsByTenChatLieuIgnoreCaseAndIdNot(String ten, Long id);

    boolean existsByMaChatLieuIgnoreCase(String ma);

    boolean existsByMaChatLieuIgnoreCaseAndIdNot(String ma, Long id);

    List<ChatLieu> findByTenChatLieuContainingIgnoreCaseOrMaChatLieuContainingIgnoreCase(String ten, String ma);
}
