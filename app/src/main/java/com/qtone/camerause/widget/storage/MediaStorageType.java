package com.qtone.camerause.widget.storage;

/**
 * Created on 2026/9/4.
 *
 * @author 郑少鹏
 * @desc 媒体存储类型
 */
public enum MediaStorageType {
    /**
     * 拍照
     */
    CAPTURE("capture", "IMG"),
    /**
     * ROI 裁剪
     */
    ROI_CROP("RoiCrop", "RC"),
    /**
     * ROI 覆盖
     */
    ROI_OVERLAY("RoiOverlay", "RO"),
    /**
     * 微信裁剪
     */
    WE_CHAT_CROP("WeChatCrop", "WC"),
    /**
     * 文档裁剪
     */
    DOCUMENT_CROP("DocumentCrop", "DC");
    /**
     * 子文件夹名
     */
    private final String subFolderName;
    /**
     * 文件名前缀
     */
    private final String fileNamePrefix;

    /**
     * constructor
     *
     * @param subFolderName  子文件夹名
     * @param fileNamePrefix 文件名前缀
     */
    MediaStorageType(String subFolderName, String fileNamePrefix) {
        this.subFolderName = subFolderName;
        this.fileNamePrefix = fileNamePrefix;
    }

    /**
     * 获取子文件夹名
     *
     * @return 子文件夹名
     */
    public String getSubFolderName() {
        return subFolderName;
    }

    /**
     * 获取文件名前缀
     *
     * @return 文件名前缀
     */
    public String getFileNamePrefix() {
        return fileNamePrefix;
    }
}