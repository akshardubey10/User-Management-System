package com.osiansoftware.usermanagementsystem;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface UserService {

    String createUser(User user);
    List<User> readUser();
    boolean deleteUser(Long id);
    String updateUser (Long id, User user);
    User readUser(Long id);

    String uploadExcel(Long usereId, MultipartFile file) throws IOException;
    byte[] downloadExcel(Long userId) throws IOException;

}
