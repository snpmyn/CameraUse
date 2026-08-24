package com.qtone.camerause.util.media;

import android.os.Handler;
import android.os.Looper;

import com.qtone.camerause.util.file.FileKit;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created on 2026/8/20.
 *
 * @author 郑少鹏
 * @desc 媒体扫描器
 */
public class MediaScanner {
    /**
     * 线程消息调度器
     */
    private final Handler handler = new Handler(Looper.getMainLooper());
    /**
     * 增强实现
     */
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * 递归扫描图片
     *
     * @param rootDirectory        根目录
     * @param mediaScannerCallback 媒体扫描器回调
     */
    public void scanImageRecursively(File rootDirectory, MediaScannerCallback mediaScannerCallback) {
        executorService.execute(() -> {
            List<FolderItem> mediaItemList = new ArrayList<>();
            if ((rootDirectory != null) && rootDirectory.exists() && rootDirectory.isDirectory()) {
                traverseDirectory(rootDirectory, mediaItemList);
            }
            handler.post(() -> mediaScannerCallback.onMediaScannerComplete(mediaItemList));
        });
    }

    /**
     * 遍历目录
     *
     * @param directory     目录
     * @param mediaItemList 媒体条目集
     */
    private void traverseDirectory(@NotNull File directory, List<FolderItem> mediaItemList) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        List<FileItem> fileItems = new ArrayList<>();
        List<File> subDirectories = new ArrayList<>();
        for (File file : files) {
            if (file.isDirectory()) {
                subDirectories.add(file);
            } else if (file.isFile() && isImageFile(file.getPath())) {
                fileItems.add(new FileItem(file));
            }
        }
        // 当前目录存在文件 -> 提取当前目录名称及文件集合 (只有存在文件才添加)
        if (!fileItems.isEmpty()) {
            mediaItemList.add(new FolderItem(directory.getName(), directory.getAbsolutePath(), fileItems));
        }
        // 递归扫描子目录
        for (File subDirectory : subDirectories) {
            traverseDirectory(subDirectory, mediaItemList);
        }
    }

    /**
     * 是否是图片文件
     *
     * @param path 路径
     * @return 是否是图片文件
     */
    private boolean isImageFile(@NotNull String path) {
        String lowerPath = path.toLowerCase();
        return lowerPath.endsWith(".jpg")
                || lowerPath.endsWith(".jpeg")
                || lowerPath.endsWith(".png")
                || lowerPath.endsWith(".webp")
                || lowerPath.endsWith(".gif");
    }

    /**
     * 媒体扫描器回调
     */
    public interface MediaScannerCallback {
        /**
         * 媒体扫描器完成
         *
         * @param mediaItemList 媒体条目集
         */
        void onMediaScannerComplete(List<FolderItem> mediaItemList);
    }

    /**
     * 媒体条目
     */
    public abstract static class MediaItem {
        /**
         * 文件夹类型
         */
        public static final int TYPE_FOLDER = 0;
        /**
         * 文件类型
         */
        public static final int TYPE_FILE = 1;

        /**
         * 获取条目类型
         *
         * @return 条目类型
         */
        public abstract int getItemType();
    }

    /**
     * 文件夹条目
     */
    public static class FolderItem extends MediaItem implements Serializable {
        /**
         * 文件夹名称
         */
        public String folderName;
        /**
         * 文件夹路径
         */
        public String folderPath;
        /**
         * 文件条目集
         */
        public List<FileItem> fileItemList;

        /**
         * constructor
         *
         * @param folderName   文件夹名称
         * @param folderPath   文件夹路径
         * @param fileItemList 文件条目集
         */
        public FolderItem(String folderName, String folderPath, List<FileItem> fileItemList) {
            this.folderName = folderName;
            this.folderPath = folderPath;
            this.fileItemList = fileItemList;
        }

        @Override
        public int getItemType() {
            return TYPE_FOLDER;
        }
    }

    /**
     * 文件条目
     */
    public static class FileItem extends MediaItem implements Serializable {
        /**
         * 文件
         */
        public File file;
        /**
         * 文件绝对路径
         */
        public String fileAbsolutePath;
        /**
         * 文件大小
         */
        public String fileSize;
        /**
         * 文件名称
         */
        public String fileName;

        /**
         * constructor
         *
         * @param file 文件
         */
        public FileItem(File file) {
            this.file = file;
            if (file != null) {
                this.fileAbsolutePath = file.getAbsolutePath();
                this.fileSize = FileKit.formatFileSize(file.length());
                this.fileName = file.getName();
            }
        }

        @Override
        public int getItemType() {
            return TYPE_FILE;
        }
    }
}