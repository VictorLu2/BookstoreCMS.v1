package com.example.bookstore.service;

import com.example.bookstore.entity.Book;
import com.example.bookstore.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BookService {
    
    private final BookRepository bookRepository;
    private static final String UPLOAD_DIR = "uploads/covers/";
    
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }
    
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }
    
    public List<Book> searchBooks(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllBooks();
        }
        // 使用已存在的 repository 方法，以標題或語言進行不分大小寫的模糊搜尋
        return bookRepository.findByTitleContainingIgnoreCaseOrLanguageContainingIgnoreCase(keyword.trim(), keyword.trim());
    }
    
    public List<Book> getBooksByStatus(Book.BookStatus status) {
        return bookRepository.findByStatus(status);
    }
    
    public List<Book> getBooksByFormat(String format) {
        // 使用已存在的忽略大小寫格式查詢方法
        return bookRepository.findByFormatIgnoreCase(format);
    }
    
    public Book createBook(Book book) {
        book.setCreatedAt(LocalDateTime.now());
        book.setUpdatedAt(LocalDateTime.now());
        return bookRepository.save(book);
    }
    
    public Book updateBook(Long id, Book bookDetails) {
        Book book = bookRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("書籍不存在"));
        
        if (bookDetails.getTitle() != null) {
            book.setTitle(bookDetails.getTitle());
        }
        if (bookDetails.getPublisherId() != null) {
            book.setPublisherId(bookDetails.getPublisherId());
        }
        if (bookDetails.getListPrice() != null) {
            book.setListPrice(bookDetails.getListPrice());
        }
        if (bookDetails.getFormat() != null) {
            book.setFormat(bookDetails.getFormat());
        }
        if (bookDetails.getStatus() != null) {
            book.setStatus(bookDetails.getStatus());
        }
        if (bookDetails.getLanguage() != null) {
            book.setLanguage(bookDetails.getLanguage());
        }
        if (bookDetails.getPublishedAt() != null) {
            book.setPublishedAt(bookDetails.getPublishedAt());
        }
        // 新增：更新封面網址
        if (bookDetails.getCoverImageUrl() != null) {
            book.setCoverImageUrl(bookDetails.getCoverImageUrl());
        }

        book.setUpdatedAt(LocalDateTime.now());
        return bookRepository.save(book);
    }
    
    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("書籍不存在"));
        bookRepository.delete(book);
    }
    
    public String uploadCoverImage(MultipartFile file) throws IOException {
        // 確保上傳目錄存在
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // 生成唯一檔名，處理原始檔名可能為 null 或無副檔名的情況
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0) {
                extension = originalFilename.substring(dot);
            }
        }
        String filename = UUID.randomUUID() + extension;

        // 儲存檔案
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);
        
        // 對外可訪問的 URL（對應 WebMvc 的 /uploads/**）
        return "/" + UPLOAD_DIR + filename;
    }
    
    public Book updateBookPrice(Long id, Integer newPrice) {
        Book book = bookRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("書籍不存在"));
        book.setListPrice(newPrice);
        book.setUpdatedAt(LocalDateTime.now());
        return bookRepository.save(book);
    }

    // 供儀表板使用：快速取得總書籍數
    public long countBooks() {
        return bookRepository.count();
    }

    // 新增：後端分頁查詢方法，支援關鍵字與狀態過濾
    public Page<Book> searchBooks(String keyword, Book.BookStatus status, Pageable pageable) {
        return bookRepository.searchForAdmin(keyword, status, pageable);
    }
}
