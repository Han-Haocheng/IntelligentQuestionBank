package com.qbank.dto;

import java.util.List;

/**
 * 批量导入保存请求
 */
public class QuestionImportSaveDTO {

    private List<ImportRowDTO> rows;
    private Long categoryId;
    private Long bankId;

    public List<ImportRowDTO> getRows() { return rows; }
    public void setRows(List<ImportRowDTO> rows) { this.rows = rows; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getBankId() { return bankId; }
    public void setBankId(Long bankId) { this.bankId = bankId; }
}
