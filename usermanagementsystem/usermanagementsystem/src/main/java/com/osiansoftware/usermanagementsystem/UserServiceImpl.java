package com.osiansoftware.usermanagementsystem;

import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
@Service
public class UserServiceImpl implements UserService{
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ExcelFileRepository excelFileRepository;

    @Autowired
    private ScoreRepository scoreRepository;

    @Override
    @Transactional
    public String createUser(User user) {
        UserEntity userEntity = new UserEntity();
        BeanUtils.copyProperties(user, userEntity);

        userRepository.save(userEntity);

        return "Saved Successfully" ;
    }

    @Override
    public List<User> readUser() {
        List<UserEntity> userList = userRepository.findByDeletedFalse();
        List<User> users = new ArrayList<>();

        for (UserEntity userEntity : userList){
            User ucs = new User();
            BeanUtils.copyProperties(userEntity, ucs);
            users.add(ucs);
        }
        return users;
    }

    @Override
    public User readUser(Long id) {
        Optional<UserEntity> optional = userRepository.findByIdAndDeletedFalse(id);

        if (optional.isEmpty()) {
            return null;
        }

        UserEntity userEntity = optional.get();
        User user = new User();
        BeanUtils.copyProperties(userEntity, user);
        return user;
    }

    @Override
    public String uploadExcel(Long userId, MultipartFile file) throws IOException {
        return uploadExcelForUser(userId, file);
    }

    @Override
    public byte[] downloadExcel(Long userId) throws IOException {
        byte[] data = getOriginalFileBytesForUser(userId);
        if (data == null) {
            throw new FileNotFoundException("Excel not found");
        }
        return data;
    }

    @Override
    @Transactional
    public String updateUser(Long id, User user) {
        Optional<UserEntity> optional = userRepository.findByIdAndDeletedFalse(id);
        if (optional.isEmpty()) {
            return "User not found";
        }
        UserEntity existingUser= optional.get();

        existingUser.setEmail(user.getEmail());
        existingUser.setName(user.getName());
        existingUser.setPhone(user.getPhone());

        userRepository.save(existingUser);
        return "Return Successfully";
    }

    @Override
    @Transactional
    public boolean deleteUser(Long id) {
        Optional<UserEntity> optional = userRepository.findByIdAndDeletedFalse(id);
        if (optional.isEmpty()) {
            return false;
        }
        UserEntity emp = optional.get();
        userRepository.delete(emp);
        return true;
    }

    @Transactional
    public String uploadExcelForUser(Long userId, MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            return "File is empty";
        }

        if (!file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
            return "Only .xlsx files are allowed";
        }

        Optional<UserEntity> optional = userRepository.findByIdAndDeletedFalse(userId);
        if (optional.isEmpty()) {
            return "User not found";
        }
        UserEntity userEntity = optional.get();

        if (excelFileRepository.findByUser_Id(userId).isPresent()) {
            return "User already has an uploaded Excel file";
        }

        List<Score> parsedScores = parseScores(file.getBytes());

        ExcelFile excelFile = new ExcelFile();
        excelFile.setOriginalFileName(file.getOriginalFilename());
        excelFile.setContentType(file.getContentType());
        excelFile.setSize(file.getSize());
        excelFile.setUploadedAt(LocalDateTime.now());
        excelFile.setData(file.getBytes());
        excelFile.setUser(userEntity);

        ExcelFile savedExcelFile = excelFileRepository.save(excelFile);

        for (Score score : parsedScores) {
            score.setExcelFile(savedExcelFile);
        }
        scoreRepository.saveAll(parsedScores);

        return "Excel uploaded and scores saved successfully";
    }

    private List<Score> parseScores(byte[] bytes) throws IOException {
        List<Score> scores = new ArrayList<>();

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes));
        Sheet sheet = workbook.getSheetAt(0);

        Row headerRow = sheet.getRow(0);
        String h0 = getStringCellValue(headerRow.getCell(0));
        String h1 = getStringCellValue(headerRow.getCell(1));
        String h2 = getStringCellValue(headerRow.getCell(2));

        if (!"Subject".equals(h0) || !"Score Obtained".equals(h1) || !"Out Of Marks".equals(h2)) {
            return scores;
        }

        int lastRowNum = sheet.getLastRowNum();
        for (int i = 1; i <= lastRowNum; i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }

            Score score = new Score();
            score.setSubject(getStringCellValue(row.getCell(0)));
            score.setScoreObtained((int) getNumericCellValue(row.getCell(1)));
            score.setOutOfMarks((int) getNumericCellValue(row.getCell(2)));

            scores.add(score);
        }

        return scores;
    }

    private String getStringCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    private double getNumericCellValue(Cell cell) {
        if (cell == null) {
            return 0;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }
        if (cell.getCellType() == CellType.STRING) {

            return Double.parseDouble(cell.getStringCellValue().trim());
        }
        return 0;
    }


    public byte[] getOriginalFileBytesForUser(Long userId) throws IOException {
        Optional<ExcelFile> optional = excelFileRepository.findByUser_Id(userId);
        if (optional.isEmpty()) {
            return null;
        }

        ExcelFile excelFile = optional.get();

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelFile.getData()));
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

}
