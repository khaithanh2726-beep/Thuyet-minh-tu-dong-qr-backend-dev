# Thuyết minh tự động QR — Backend DEV

Backend đồ án Công nghệ phần mềm cung cấp API hiện vật, bản dịch, QR, tài khoản và audio VoiceRSS. Repository chỉ triển khai backend theo Controller → Service → Repository; Swagger UI dùng để kiểm tra API.

Luồng tích hợp: đọc mã QR → lấy thông tin và ngôn ngữ hiện vật qua API → gọi API với ngôn ngữ đã chọn → nhận nội dung cùng `audioUrl` → tải MP3 từ `audioUrl`. Việc hiển thị nút chọn ngôn ngữ và trình phát thuộc ứng dụng sử dụng API.

Luồng nghe trực tiếp bằng điện thoại: chọn hiện vật và ngôn ngữ trong Swagger → tải QR riêng cho ngôn ngữ đó → quét QR → trình duyệt mở MP3 bằng trình phát có sẵn. Có thể cần bấm ▶ để bắt đầu nghe. Backend không cung cấp trang chọn ngôn ngữ sau khi quét.

Java 21 · Spring Boot 4.1.1 · Maven · MySQL · Flyway · Spring Security/JWT · VoiceRSS · ZXing · Docker · GitHub Actions.

## Chạy bằng Docker trên Windows

Yêu cầu: Docker Desktop đang chạy Linux containers, PowerShell và Internet để tải image/dependency, gọi VoiceRSS.

1. Mở PowerShell tại thư mục gốc repository.
2. Tạo cấu hình riêng cho máy DEV:

   ```powershell
   .\scripts\Initialize-Dev.ps1
   ```

   Script tạo `qr-backend/.env`, tự sinh mật khẩu database, JWT và mật khẩu Admin; giữ nguyên file nếu đã tồn tại. Khóa VoiceRSS được đọc từ cấu hình riêng hiện có hoặc nhập ẩn. Không in giá trị bí mật ra màn hình.

   Nếu cổng 8080 đang được dùng, chọn cổng khác ngay lần tạo đầu:

   ```powershell
   .\scripts\Initialize-Dev.ps1 -Port 8081
   ```

3. Mở riêng `qr-backend/.env` bằng trình soạn thảo. Kiểm tra `APP_PORT`, `PUBLIC_BASE_URL`, `VOICERSS_API_KEY`. Để quét bằng điện thoại, đặt `PUBLIC_BASE_URL=http://<IP-LAN-của-máy>:<APP_PORT>`. Hai thiết bị cần cùng Wi-Fi; máy DEV cần cho phép kết nối đến cổng ứng dụng. Script thử chọn IP LAN; nếu chỉ tìm được localhost thì cần nhập IP thủ công.
4. Khởi động:

   ```powershell
   cd qr-backend
   docker compose up -d --build --wait --wait-timeout 240
   docker compose ps
   ```

   Kết quả mong đợi: `db` và `app` ở trạng thái healthy.
5. Mở `http://localhost:<APP_PORT>/swagger-ui/index.html` để thử API; `/api/health` để xem trạng thái database. Backend không cung cấp trang chủ, trang khách hay trang Admin.
6. Admin ban đầu: tên ở `BOOTSTRAP_ADMIN_USERNAME`, mật khẩu ở `BOOTSTRAP_ADMIN_PASSWORD` trong file `.env` của máy. Tài khoản chỉ được tạo khi bảng tài khoản còn trống; khởi động lại không đặt lại mật khẩu. Thay biến bootstrap không thay đổi tài khoản đã tồn tại.

Database Docker là môi trường riêng, không chép hoặc ghi đè database MySQL đang chạy ngoài Docker. Volume `qr-audio-mvp_mysql_mvp_data` và `qr-audio-mvp_audio_mvp_data` giữ dữ liệu qua các lần khởi động. Không dùng `down -v` nếu muốn giữ dữ liệu. Tài khoản MySQL ứng dụng là `qr_app`; cổng database chỉ mở trên localhost, mặc định 3308.

Tắt và chạy lại, từ thư mục `qr-backend`:

```powershell
docker compose stop
docker compose up -d --wait --wait-timeout 240
```

## Dữ liệu mẫu và cách dùng

Ứng dụng thêm ba hiện vật minh họa khi khởi động với `SEED_DEMO_DATA=true`. Mỗi hiện vật có bản Việt (`vi-vn`), Anh (`en-us`) và một QR:

| Mã QR | Hiện vật | API được mã hóa trong QR |
| --- | --- | --- |
| `DEMO-OIL-LAMP` | Đèn dầu | `/api/qrcodes/DEMO-OIL-LAMP` |
| `DEMO-BRONZE-DRUM` | Trống đồng | `/api/qrcodes/DEMO-BRONZE-DRUM` |
| `DEMO-CERAMIC-VASE` | Bình gốm | `/api/qrcodes/DEMO-CERAMIC-VASE` |

Nội dung và SVG là dữ liệu trình diễn, không phải hồ sơ khoa học của hiện vật thật. Mẫu đã tồn tại sẽ được giữ nguyên, kể cả bản dịch đã sửa hoặc QR đã vô hiệu hóa. Các ID được database tự cấp. Dữ liệu từ migration cũ vẫn được giữ nên bộ sưu tập có thể có thêm hiện vật cũ.

PNG tải từ `GET /api/qrcodes/{code}/image` không có `lang` chứa URL tuyệt đối của API metadata trong bảng. API metadata trả các bản dịch có sẵn và không gọi VoiceRSS. Thêm `?lang=vi-vn` hoặc `?lang=en-us` để tạo QR nghe trực tiếp cho ngôn ngữ đó. URL trong QR lúc này là `/api/qrcodes/{code}/audio?lang=...`, ghép với `PUBLIC_BASE_URL`. Một mã hiện vật có nhiều ảnh QR theo ngôn ngữ, không cần tạo thêm bản ghi database. Tạo PNG không gọi VoiceRSS.

Đổi `PUBLIC_BASE_URL` cần tải lại PNG; ảnh đã in không tự đổi địa chỉ. Quét ảnh không có `lang` mở JSON; quét ảnh có `lang` mở MP3. Sửa nội dung bản dịch vẫn dùng được QR cũ và tạo audio mới; vô hiệu hóa QR/hiện vật hoặc xóa bản dịch làm URL nghe trả 404. Ngôn ngữ không hợp lệ/không được hỗ trợ trả 400.

### Tạo QR Việt/Anh và quét bằng điện thoại

1. Trong Swagger, gọi `GET /api/catalog`, chọn hiện vật và lấy `data[].qrCode`. Danh sách `languages` của hiện vật cho biết những bản dịch có sẵn.
2. Mở nhóm **Public - QR**, chọn `GET /api/qrcodes/{code}/image`, bấm **Try it out**. Nhập `code=DEMO-OIL-LAMP`, `lang=vi-vn`, bấm **Execute**. Mở/tải PNG từ response để hiển thị trên máy tính.
3. Làm lại với `lang=en-us` để lấy ảnh QR tiếng Anh. Hai ảnh chứa hai URL nghe khác nhau. Có thể mở trực tiếp trên trình duyệt máy tính: `http://localhost:<APP_PORT>/api/qrcodes/DEMO-OIL-LAMP/image?lang=vi-vn` hoặc đổi thành `lang=en-us`.
4. Điện thoại cùng Wi-Fi mở thử `http://<IP-LAN-của-máy>:<APP_PORT>/api/health`, phải thấy `UP`. Nếu không truy cập được, kiểm tra IP Wi-Fi thực tế, cổng, tường lửa và khả năng kết nối giữa hai thiết bị. Không dùng localhost hoặc IP VPN trong `PUBLIC_BASE_URL` khi điện thoại kết nối qua Wi-Fi.
5. Quét ảnh QR tiếng Việt hoặc Anh bằng camera điện thoại, mở liên kết và bấm ▶ nếu trình duyệt yêu cầu. Backend trả MP3 của bản dịch đã chọn, dùng lại file có sẵn. Đây là trình phát của trình duyệt điện thoại, không có trang frontend riêng.

Nếu hiện vật chưa có QR, Admin tạo bằng `POST /api/admin/qrcodes` với `exhibitId` và mã tùy chọn; lấy `data.code` để làm bước 2. Admin cũng có thể lấy ảnh qua `GET /api/admin/qrcodes/{id}/image?lang=vi-vn`.

Để lấy bản Việt và audio, gọi `GET /api/qrcodes/resolve/DEMO-OIL-LAMP?lang=vi-vn`; đổi thành `lang=en-us` để lấy bản Anh. Response có `id`, `imageUrl`, `languageCode`, `title`, `content`, `audioUrl` trong `data`. `audioUrl` là đường dẫn tương đối trên cùng backend; gọi đường dẫn này để nhận MP3. Backend không tự phát âm thanh từ response JSON.

Lần đầu gọi VoiceRSS, những lần sau dùng MP3 cục bộ. Đổi nội dung bản dịch tạo audio mới; sửa tên mà giữ nội dung không tạo lại audio. File audio cũ được giữ, chưa có tác vụ tự động dọn file không còn dùng.

Quản trị thông qua API có JWT:

- **Hiện vật và bản dịch**: tạo/sửa/vô hiệu hóa hiện vật; thêm/sửa/xóa bản dịch. Ảnh nhận URL hoặc đường dẫn ảnh có sẵn, chưa có API upload ảnh.
- **QR**: tạo mã tự động hoặc nhập mã riêng, gắn hiện vật, tải PNG, kích hoạt/vô hiệu hóa. Response có `qrUrl` là URL metadata và `imageUrl` là đường dẫn API tải ảnh dành cho Admin. Thêm `?lang=...` vào đường dẫn ảnh để tạo QR mở MP3 thay cho metadata.
- **Tài khoản**: tạo Admin, đổi thông tin/mật khẩu, kích hoạt/vô hiệu hóa. Không được vô hiệu hóa Admin hoạt động cuối cùng. Thay đổi tài khoản thu hồi JWT cũ.

## API chính

API JSON dùng cấu trúc `{status,message,data,timestamp}`. Endpoint ảnh/audio trả dữ liệu nhị phân; health trả `{status:"UP"}`.

| Nhóm | Endpoint | Quyền |
| --- | --- | --- |
| Danh sách hiện vật | `GET /api/catalog` | Công khai, không tạo audio |
| Nội dung/ngôn ngữ hiện vật | `GET /api/exhibits/{id}/info` | Công khai, không tạo audio |
| Nội dung/ngôn ngữ từ QR | `GET /api/qrcodes/{code}` | Công khai, không tạo audio |
| Nội dung và audio từ QR | `GET /api/qrcodes/resolve/{code}?lang=vi-vn` | Công khai |
| Nội dung và audio theo ID | `GET /api/exhibits/{id}?lang=en-us` | Công khai |
| Ngôn ngữ VoiceRSS hỗ trợ | `GET /api/languages` | Công khai; dùng metadata hiện vật để biết bản dịch thực có |
| Ảnh QR PNG | `GET /api/qrcodes/{code}/image` | QR và hiện vật đang hoạt động |
| Ảnh QR nghe theo ngôn ngữ | `GET /api/qrcodes/{code}/image?lang=vi-vn` | Công khai; cần bản dịch tương ứng |
| Nghe trực tiếp từ QR | `GET /api/qrcodes/{code}/audio?lang=vi-vn` | Công khai; GET/HEAD, hỗ trợ Range; cần QR, hiện vật và bản dịch đang khả dụng |
| Phát MP3 | `GET /api/audio/play/{fileName}` | Công khai, hỗ trợ tua qua Range |
| Đăng nhập | `POST /api/auth/login` | Công khai |
| Hiện vật | `/api/admin/exhibits` | Admin |
| Bản dịch | `/api/admin/exhibits/{id}/translations` | Admin |
| QR | `/api/admin/qrcodes` | Admin |
| Tài khoản | `/api/admin/users` | Admin |

Swagger mô tả phương thức và request body. Sau đăng nhập, lấy `data.token`, bấm **Authorize** và nhập JWT. Các API quản trị trả 401 nếu chưa đăng nhập, 403 nếu thiếu quyền. Hiện vật/QR không hoạt động hoặc thiếu bản dịch trả 404. VoiceRSS lỗi trả 502, thiếu khóa hoặc không lưu được file trả 503; không trả `audioUrl:null` âm thầm.

Nghe thử ngay trong Swagger: mở nhóm **Public - QR**, gọi `GET /api/qrcodes/{code}/audio` với `code` và `lang`, bấm **Execute**. Thanh phát MP3 xuất hiện trong Response body. Cách cũ vẫn dùng được: gọi `resolve`, lấy tên file cuối `data.audioUrl` rồi dùng `GET /api/audio/play/{fileName}`. API tạo bản dịch chỉ chấp nhận ngôn ngữ trong `GET /api/languages`.

## Chạy Java trực tiếp

Tại `qr-backend`, dùng Java 21 và MySQL với database đã tạo. Cấu hình qua biến môi trường `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (Base64 tối thiểu 32 byte), `VOICERSS_API_KEY`; có thể dùng file riêng `dev-local.properties` theo các tên thuộc tính Spring `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`, `security.jwt.secret`, `voicerss.api-key`. File riêng được đọc từ thư mục chạy hiện tại và không được commit. Cấu hình Docker dùng `.env`, không dùng file properties của máy.

```powershell
.\mvnw.cmd spring-boot:run
```

Flyway tự áp dụng migration; Hibernate chỉ validate. Không sửa migration đã áp dụng. Khi dùng database trống, đặt thêm `BOOTSTRAP_ADMIN_USERNAME`/`BOOTSTRAP_ADMIN_PASSWORD` để tạo Admin ban đầu. Cấu hình DEV riêng hiện có được giữ lại trên máy, không cần chép bí mật vào README hay mã nguồn.

Khi chạy Java trực tiếp, dữ liệu mẫu mặc định tắt; đặt `SEED_DEMO_DATA=true` nếu cần. Docker DEV đã bật biến này. Thay đổi nội dung seed trong code không ghi đè các mẫu đã có trong database.

Nếu đã có `dev-local.properties`, các thuộc tính trực tiếp trong file đó được ưu tiên hơn giá trị cấu hình tương ứng ở `application.yml`. Khi muốn chuyển sang dùng các biến `DB_URL`/`JWT_SECRET` của `application.yml`, hãy bỏ thuộc tính tương ứng khỏi file riêng hoặc cập nhật trực tiếp file riêng.

## Kiểm thử và nghiệm thu

Từ `qr-backend`:

```powershell
.\mvnw.cmd verify
```

Test tự động dùng H2 với profile `test`, không dùng database DEV và không gọi VoiceRSS thật. Có kiểm thử JWT/đăng nhập, phân quyền, tài khoản, hiện vật/bản dịch, QR CRUD và giải mã PNG metadata/audio Việt/Anh, phát MP3 trực tiếp và Range/HEAD, seed lặp lại, lỗi VoiceRSS, audio cache và yêu cầu đồng thời, chặn đường dẫn ngoài thư mục audio. CI kiểm tra thêm Docker với MySQL/Flyway thật.

Kiểm tra thực tế một lượt:

Từ thư mục gốc, có thể kiểm tra nhanh stack và sáu audio mẫu bằng lệnh dưới (thay cổng theo `.env`). `-WithAudio` sử dụng hạn mức VoiceRSS nếu audio chưa có:

```powershell
.\scripts\Test-Dev.ps1 -BaseUrl http://localhost:8081 -WithAudio
```

1. Gọi `GET /api/catalog` và `GET /api/qrcodes/DEMO-OIL-LAMP`: có ba mẫu mới, mỗi mẫu có bản Việt và Anh. Database có thể giữ thêm dữ liệu migration cũ.
2. Gọi `resolve` với `lang=vi-vn`, rồi `lang=en-us`: kiểm tra đúng `languageCode`, `title`, `content` và `audioUrl`. Mở URL audio trên cùng host/cổng để nghe MP3.
3. Gọi lại cùng bản dịch: URL và file audio được dùng lại. Dừng/chạy lại Docker rồi gọi lại để xác nhận dữ liệu volume được giữ.
4. Trong Swagger, đăng nhập và Authorize; tạo hiện vật, thêm hai bản dịch, tạo QR, tải PNG. URL giải mã từ PNG phải bằng `data.qrUrl` và trả metadata hiện vật.
   Tải thêm PNG với `lang=vi-vn` và `lang=en-us`: URL giải mã phải kết thúc bằng `/audio?lang=vi-vn` và `/audio?lang=en-us`; quét bằng điện thoại để nghe đúng ngôn ngữ.
5. Sửa nội dung bản dịch rồi resolve lại: có URL audio mới. Vô hiệu hóa QR hoặc hiện vật rồi gọi public API: trả 404. Thiếu bản dịch cũng trả 404.
6. Tạo Admin thứ hai, đăng nhập thử; kiểm tra không thể khóa Admin hoạt động cuối cùng. Xóa JWT khỏi Authorize rồi gọi API admin: trả 401. JWT không có quyền Admin phải nhận 403.

Ví dụ PowerShell, chạy ở bất kỳ thư mục nào (thay cổng theo cấu hình):

```powershell
$baseUrl = 'http://localhost:8081'
$metadata = Invoke-RestMethod "$baseUrl/api/qrcodes/DEMO-OIL-LAMP"
$metadata.data.exhibit.languages | Select-Object code, name, title
$narration = Invoke-RestMethod "$baseUrl/api/qrcodes/resolve/DEMO-OIL-LAMP?lang=vi-vn"
$narration.data | Format-List
Start-Process ($baseUrl + $narration.data.audioUrl)
```

## CI/CD GitHub Actions — task #22

Workflow `.github/workflows/ci-cd.yml` chạy khi push/PR vào `main`, hoặc chạy thủ công:

1. Build và chạy toàn bộ test Maven.
2. Build Docker, chạy MySQL/Flyway, kiểm tra health, metadata mẫu và PNG QR.
3. Sau CI thành công trên `main`, build/push image có tag commit SHA vào GHCR bằng `GITHUB_TOKEN`.
4. Nếu repository variable `DEV_DEPLOY_ENABLED=true`, triển khai image đó lên Docker máy DEV bằng self-hosted runner Windows có nhãn `qr-dev` và đợi health thành công. PR không publish/deploy.

Thiết lập CD trên máy của bạn:

1. GitHub repository → **Settings → Actions → Runners → New self-hosted runner** → Windows x64. Làm theo lệnh đăng ký GitHub cung cấp; thêm nhãn `qr-dev`. Đặt runner ở thư mục riêng ngoài repository. Chạy runner dưới tài khoản có quyền dùng Docker Desktop, khi Docker Desktop đang mở.
2. Chép file `.env` riêng sang thư mục ổn định ngoài checkout để runner không xóa cấu hình khi checkout:

   ```powershell
   New-Item -ItemType Directory -Force "$env:USERPROFILE\.qr-audio-dev"
   Copy-Item -LiteralPath .\qr-backend\.env -Destination "$env:USERPROFILE\.qr-audio-dev\.env"
   ```

   Nếu dùng tài khoản Windows khác chạy runner, đặt repository variable `DEV_ENV_FILE` tới đường dẫn tuyệt đối của file `.env` đó.
3. **Settings → Secrets and variables → Actions → Variables**: thêm `DEV_DEPLOY_ENABLED` bằng `true`. Package GHCR cần cho phép repository Actions đọc/ghi package nếu tổ chức/tài khoản có hạn chế riêng.
4. Tự commit/push code rồi mở **Actions → CI-CD**. Chỉ coi task CI/CD nghiệm thu khi CI, publish image và deploy thực sự xanh. Chưa đăng ký runner thì giữ biến bật deploy ở trạng thái chưa đặt/false; CI và đóng gói vẫn chạy, deploy được skip.

Nếu deploy thất bại, job báo thất bại và giữ database/audio volume; chưa có tự động rollback. Có thể chạy `scripts/Deploy-Dev.ps1 -Image <tag-SHA-trước> -EnvFile <file-riêng>` để triển khai lại image tương thích schema trước đó. Không tự rollback migration database.

`.env`, `dev-local.properties`, `audio_cache` và `target` đã được bỏ qua bởi Git; Docker build loại cấu hình riêng. Không in/chụp/chia sẻ nội dung các file chứa bí mật. PRD vẫn là tài liệu thiết kế tổng hợp của đồ án; README này hướng dẫn chạy và nghiệm thu.
