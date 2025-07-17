package top.cywin.onetv.movie.cloudrive.bean;

/**
 * 网盘文件数据模型
 * 基于FongMi_TV架构设计
 */
public class CloudFile {
    public static final int TYPE_FILE = 0;
    public static final int TYPE_FOLDER = 1;
    
    private String id;
    private String name;
    private String path;
    private long size;
    private int type;
    private String modified;
    private String downloadUrl;
    private String thumbnailUrl;
    private String mimeType;
    
    public CloudFile() {
    }
    
    public CloudFile(String id, String name, String path, long size, int type) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.size = size;
        this.type = type;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public long getSize() {
        return size;
    }
    
    public void setSize(long size) {
        this.size = size;
    }
    
    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.type = type;
    }
    
    public String getModified() {
        return modified;
    }
    
    public void setModified(String modified) {
        this.modified = modified;
    }
    
    public String getDownloadUrl() {
        return downloadUrl;
    }
    
    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
    
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
    
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    
    public boolean isFolder() {
        return type == TYPE_FOLDER;
    }
    
    public boolean isFile() {
        return type == TYPE_FILE;
    }
    
    public boolean isVideoFile() {
        if (mimeType != null) {
            return mimeType.startsWith("video/");
        }
        if (name != null) {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") || 
                   lowerName.endsWith(".avi") || lowerName.endsWith(".mov") ||
                   lowerName.endsWith(".wmv") || lowerName.endsWith(".flv") ||
                   lowerName.endsWith(".webm") || lowerName.endsWith(".m4v");
        }
        return false;
    }
    
    @Override
    public String toString() {
        return "CloudFile{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", size=" + size +
                ", type=" + type +
                ", modified='" + modified + '\'' +
                '}';
    }
}
