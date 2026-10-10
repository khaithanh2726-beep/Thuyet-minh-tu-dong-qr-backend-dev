package com.qraudio.backend.service;

import com.qraudio.backend.dto.CreateUserRequest;
import com.qraudio.backend.dto.UpdateUserRequest;
import com.qraudio.backend.dto.UserResponse;
import com.qraudio.backend.entity.User;
import com.qraudio.backend.exception.ApiException;
import com.qraudio.backend.exception.ResourceNotFoundException;
import com.qraudio.backend.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return userRepository.findAll(Sort.by("id")).stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) { return UserResponse.from(findUser(id)); }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "Tên đăng nhập đã tồn tại");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(encodePassword(request.password()));
        user.setRole("ADMIN");
        user.setActive(true);
        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        if (request.username() == null && request.password() == null
                && request.role() == null && request.active() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Chưa có thông tin cần cập nhật");
        }
        List<User> admins = userRepository.findAdminsForUpdate();
        User user = findUser(id);
        if (Boolean.FALSE.equals(request.active())) { ensureAnotherAdmin(user, admins); }
        boolean changed = false;
        if (request.username() != null && !request.username().equals(user.getUsername())) {
            if (userRepository.existsByUsernameIgnoreCaseAndIdNot(request.username(), id)) {
                throw new ApiException(HttpStatus.CONFLICT, "Tên đăng nhập đã tồn tại");
            }
            user.setUsername(request.username());
            changed = true;
        }
        if (request.password() != null) {
            user.setPasswordHash(encodePassword(request.password()));
            changed = true;
        }
        if (request.role() != null && !request.role().equals(user.getRole())) {
            user.setRole(request.role());
            changed = true;
        }
        if (request.active() != null && request.active() != user.isActive()) {
            user.setActive(request.active());
            changed = true;
        }
        if (changed) { user.invalidateTokens(); }
        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    @Transactional
    public void deactivate(Long id) {
        List<User> admins = userRepository.findAdminsForUpdate();
        User user = findUser(id);
        ensureAnotherAdmin(user, admins);
        if (user.isActive()) {
            user.setActive(false);
            user.invalidateTokens();
            userRepository.saveAndFlush(user);
        }
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
    }

    private void ensureAnotherAdmin(User user, List<User> admins) {
        if (user.isActive() && "ADMIN".equals(user.getRole())
                && admins.stream().filter(User::isActive).count() <= 1) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Không thể vô hiệu hóa tài khoản Admin hoạt động cuối cùng");
        }
    }

    private String encodePassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Mật khẩu không được vượt quá 72 byte UTF-8");
        }
        return passwordEncoder.encode(password);
    }
}
