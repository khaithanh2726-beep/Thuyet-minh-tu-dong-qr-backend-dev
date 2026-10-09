# Sử dụng môi trường Java 21 chính thức làm nền tảng
FROM eclipse-temurin:21-jdk-alpine

# Thiết lập thư mục làm việc bên trong container
WORKDIR /app

# Copy thư mục mã nguồn và file cấu hình Maven vào container
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
COPY src ./src

# Đảm bảo file mvnw có quyền thực thi
RUN chmod +x mvnw

# Yêu cầu Maven tải thư viện và biên dịch
RUN ./mvnw clean package -Dmaven.test.skip=true

# Mở cổng 8080 để giao tiếp với bên ngoài
EXPOSE 8080

# Lệnh khởi chạy ứng dụng khi container bắt đầu
CMD ["java", "-jar", "target/qr-backend-0.0.1-SNAPSHOT.jar"]