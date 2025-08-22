package com.example.bookstore.repository;

import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Book.BookStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    // 狀態查詢
    List<Book> findByStatus(BookStatus status);

    // 模糊搜尋（書名 或 語言，不分大小寫）
    List<Book> findByTitleContainingIgnoreCaseOrLanguageContainingIgnoreCase(String title, String language);

    // 格式查詢
    List<Book> findByFormatIgnoreCase(String format);

    // 出版社查詢（以欄位 publisherId 查詢）
    List<Book> findByPublisherId(Long publisherId);

    @Query("SELECT b FROM Book b WHERE (:keyword IS NULL OR :keyword = '' OR lower(b.title) LIKE lower(concat('%', :keyword, '%')) OR lower(b.language) LIKE lower(concat('%', :keyword, '%'))) " +
           "AND (:status IS NULL OR b.status = :status)")
    Page<Book> searchForAdmin(@Param("keyword") String keyword,
                              @Param("status") BookStatus status,
                              Pageable pageable);
}
