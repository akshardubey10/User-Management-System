package com.osiansoftware.usermanagementsystem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
@RestController
public class UserController {
    @Autowired
    UserService userService;

    @GetMapping("/user")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.readUser();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        User ucs = userService.readUser(id);

        if (ucs == null) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(ucs, HttpStatus.OK);

    }

    @PostMapping("/user")
    public ResponseEntity<String> createUser(@RequestBody User user) {
        String result = userService.createUser(user);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @DeleteMapping("/user/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        boolean deleted = userService.deleteUser(id);

        if (deleted) {
            return new ResponseEntity<>("Deleted Successfully", HttpStatus.OK);
        }

        return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
    }

    @PutMapping("/user/{id}")
    public ResponseEntity<String> updateUser(@PathVariable Long id, @RequestBody User user) {
        String result = userService.updateUser(id, user);

        if (result.equals("Not Found")) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/user/{id}/excel")
    public ResponseEntity<String> uploadExcel(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws Exception {

        String result = userService.uploadExcel(id, file);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/user/{id}/excel")
    public ResponseEntity<byte[]> downloadExcel(@PathVariable Long id) throws IOException {
        byte[] bytes = userService.downloadExcel(id);
        if (bytes == null || bytes.length == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        );

        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"user-" + id + "-scores.xlsx\"");

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

}
