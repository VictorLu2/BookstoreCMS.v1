# 書店管理系統 (Bookstore Management System)

## 專案概述

這是一個基於 Spring Boot 的書籍電商平台後台管理系統，提供完整的會員管理、書籍管理、訂單管理、點數系統、優惠券系統等功能。

## 技術架構

- **後端框架**: Spring Boot 3.5.4
- **資料庫**: MySQL 8.0+
- **安全框架**: Spring Security
- **前端模板**: Thymeleaf
- **樣式框架**: Bootstrap 5.1.3
- **圖表庫**: Chart.js 3.7.0
- **圖示庫**: Font Awesome 6.0.0
- **Java 版本**: 21
- **建構工具**: Maven

## 功能特色

### 1. 會員管理
- 會員列表檢視
- 搜尋與篩選會員
- 停權/啟用會員
- 會員資料編輯

### 2. 書籍商品管理
- 新增/編輯/刪除書籍
- 上傳書籍封面圖片
- 書籍狀態管理
- 書籍搜尋與篩選

### 3. 銷售訂單管理
- 全會員訂單查詢
- 訂單狀態管理
- 訂單搜尋功能
- 訂單統計報表

### 4. 點數系統
- 點數規則設定
- 消費回饋計算
- 點數折抵計算
- 點數到期管理

### 5. 優惠券系統
- 優惠券創建與管理
- 折扣類型支援（百分比/固定金額）
- 使用限制設定
- 有效期管理

### 6. 統計報表儀表板
- 銷售金額統計
- 訂單數量趨勢
- 熱銷商品排行
- 操作統計圖表

### 7. 操作紀錄系統
- 管理員操作日誌
- 操作類型分類
- 詳細操作記錄
- 操作趨勢分析

## 資料庫設計

系統包含以下主要資料表：

- `roles` - 角色表
- `users` - 使用者表
- `books` - 書籍表
- `book_skus` - 書籍SKU表
- `orders` - 訂單表
- `order_items` - 訂單項目表
- `points_accounts` - 點數帳戶表
- `point_rules` - 點數規則表
- `coupons` - 優惠券表
- `admin_logs` - 管理員操作日誌表

## 安裝與設定

### 前置需求
- Java 21+
- MySQL 8.0+
- Maven 3.6+

### 資料庫設定
1. 創建 MySQL 資料庫：
```sql
CREATE DATABASE bookstore CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 修改 `src/main/resources/application.properties` 中的資料庫連線設定：
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/bookstore?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Taipei
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 執行專案
1. 克隆專案：
```bash
git clone <repository-url>
cd bookstore
```

2. 編譯專案：
```bash
mvn clean compile
```

3. 執行專案：
```bash
mvn spring-boot:run
```

4. 開啟瀏覽器訪問：`http://localhost:8080`

## 預設帳號

- **管理員帳號**: admin@bookstore.com
- **預設密碼**: admin123

## 專案結構

```
src/
├── main/
│   ├── java/com/example/bookstore/
│   │   ├── config/          # 配置類別
│   │   ├── controller/      # 控制器
│   │   ├── entity/          # 實體類別
│   │   ├── repository/      # 資料存取層
│   │   ├── service/         # 業務邏輯層
│   │   └── BookstoreApplication.java
│   └── resources/
│       ├── static/          # 靜態資源
│       ├── templates/       # Thymeleaf 模板
│       ├── application.properties
│       └── schema.sql      # 資料庫初始化腳本
```

## API 端點

### 管理員功能
- `GET /admin/dashboard` - 儀表板
- `GET /admin/users` - 會員管理
- `GET /admin/books` - 書籍管理
- `GET /admin/orders` - 訂單管理
- `GET /admin/points` - 點數管理
- `GET /admin/coupons` - 優惠券管理
- `GET /admin/logs` - 操作紀錄

### 認證功能
- `GET /login` - 登入頁面
- `POST /login` - 登入處理
- `POST /logout` - 登出處理

## 點數系統邏輯

### 消費回饋
- 預設回饋比率：1%（100 bp）
- 可設定每筆回饋上限
- 支援滾動天數到期（預設180天）

### 點數折抵
- 1點 = 1元（可調整）
- 可設定折抵上限（預設50%）
- 支援最低消費限制

## 優惠券系統邏輯

### 折扣類型
- 百分比折扣：如 10% 折扣
- 固定金額折扣：如 $50 折扣

### 使用限制
- 總使用次數限制
- 每人使用次數限制
- 最低消費金額限制
- 最大折扣金額限制

## 開發說明

### 新增功能
1. 在 `entity` 包中創建實體類別
2. 在 `repository` 包中創建資料存取介面
3. 在 `service` 包中實作業務邏輯
4. 在 `controller` 包中處理 HTTP 請求
5. 在 `templates` 包中創建前端頁面

### 資料庫變更
1. 修改實體類別
2. 更新 `schema.sql` 腳本
3. 重新執行資料庫初始化

## 部署說明

### 打包 JAR 檔案
```bash
mvn clean package
```

### 執行 JAR 檔案
```bash
java -jar target/bookstore-0.0.1-SNAPSHOT.jar
```

### 生產環境設定
建議修改以下設定：
- 資料庫連線池大小
- 日誌級別
- 檔案上傳大小限制
- 安全性設定

## 注意事項

1. 首次執行時會自動創建資料表結構
2. 預設管理員帳號會在資料庫初始化時自動創建
3. 上傳的圖片檔案會儲存在 `uploads/covers/` 目錄
4. 建議在生產環境中設定適當的資料庫連線池和快取設定

## 授權

本專案僅供學習和研究使用。

## 聯絡資訊

如有問題或建議，請聯繫開發團隊。
