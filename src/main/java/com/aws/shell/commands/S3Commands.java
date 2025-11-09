package com.aws.shell.commands;

import com.aws.shell.util.OutputFormatter;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * S3 Commands
 * <p>
 * Implements AWS S3 CLI-like functionality
 */
@ShellComponent
public class S3Commands {

    private final S3Client s3Client;

    public S3Commands(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * List S3 buckets or objects in a bucket
     * <p>
     * Usage:
     * s3 ls                    - List all buckets
     * s3 ls s3://bucket-name   - List objects in bucket
     * s3 ls s3://bucket/prefix - List objects with prefix
     */
    @ShellMethod(key = "s3 ls", value = "List S3 buckets or objects")
    public String list(@ShellOption(defaultValue = "") String path) {
        try {
            if (path.isEmpty()) {
                // List all buckets
                return listBuckets();
            } else {
                // Parse s3://bucket/prefix
                S3Path s3Path = parseS3Path(path);
                return listObjects(s3Path.bucket, s3Path.prefix);
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Create a new S3 bucket
     * <p>
     * Usage:
     * s3 mb s3://bucket-name
     */
    @ShellMethod(key = "s3 mb", value = "Create an S3 bucket")
    public String makeBucket(String bucketUri) {
        try {
            S3Path s3Path = parseS3Path(bucketUri);

            CreateBucketRequest request = CreateBucketRequest.builder()
                    .bucket(s3Path.bucket)
                    .build();

            s3Client.createBucket(request);
            return "Bucket created: " + s3Path.bucket;
        } catch (Exception e) {
            return "Error creating bucket: " + e.getMessage();
        }
    }

    /**
     * Remove an S3 bucket
     * <p>
     * Usage:
     * s3 rb s3://bucket-name
     */
    @ShellMethod(key = "s3 rb", value = "Remove an S3 bucket")
    public String removeBucket(String bucketUri) {
        try {
            S3Path s3Path = parseS3Path(bucketUri);

            DeleteBucketRequest request = DeleteBucketRequest.builder()
                    .bucket(s3Path.bucket)
                    .build();

            s3Client.deleteBucket(request);
            return "Bucket deleted: " + s3Path.bucket;
        } catch (Exception e) {
            return "Error deleting bucket: " + e.getMessage();
        }
    }

    /**
     * Copy files to/from S3
     * <p>
     * Usage:
     * s3 cp local-file s3://bucket/key     - Upload file
     * s3 cp s3://bucket/key local-file     - Download file
     * s3 cp s3://bucket1/key s3://bucket2/key - Copy between buckets
     */
    @ShellMethod(key = "s3 cp", value = "Copy files to/from S3")
    public String copy(String source, String destination) {
        try {
            boolean sourceIsS3 = source.startsWith("s3://");
            boolean destIsS3 = destination.startsWith("s3://");

            if (!sourceIsS3 && destIsS3) {
                // Upload local file to S3
                return uploadFile(source, destination);
            } else if (sourceIsS3 && !destIsS3) {
                // Download S3 file to local
                return downloadFile(source, destination);
            } else if (sourceIsS3 && destIsS3) {
                // Copy between S3 locations
                return copyS3Object(source, destination);
            } else {
                return "Error: At least one path must be an S3 URI (s3://...)";
            }
        } catch (Exception e) {
            return "Error copying: " + e.getMessage();
        }
    }

    /**
     * Remove S3 objects
     * <p>
     * Usage:
     * s3 rm s3://bucket/key
     */
    @ShellMethod(key = "s3 rm", value = "Remove S3 objects")
    public String remove(String path) {
        try {
            S3Path s3Path = parseS3Path(path);

            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(s3Path.bucket)
                    .key(s3Path.key)
                    .build();

            s3Client.deleteObject(request);
            return "Object deleted: " + path;
        } catch (Exception e) {
            return "Error deleting object: " + e.getMessage();
        }
    }

    // Helper methods

    private String listBuckets() {
        ListBucketsResponse response = s3Client.listBuckets();

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Name", "Creation Date"});

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Bucket bucket : response.buckets()) {
            rows.add(new String[]{
                    bucket.name(),
                    bucket.creationDate().atZone(ZoneId.systemDefault()).format(formatter)
            });
        }

        return OutputFormatter.toTable(rows);
    }

    private String listObjects(String bucket, String prefix) {
        ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                .bucket(bucket);

        if (prefix != null && !prefix.isEmpty()) {
            requestBuilder.prefix(prefix);
        }

        ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Key", "Size", "Last Modified"});

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (S3Object object : response.contents()) {
            rows.add(new String[]{
                    object.key(),
                    formatSize(object.size()),
                    object.lastModified().atZone(ZoneId.systemDefault()).format(formatter)
            });
        }

        if (rows.size() == 1) {
            return "No objects found";
        }

        return OutputFormatter.toTable(rows);
    }

    private String uploadFile(String localPath, String s3Uri) throws Exception {
        S3Path s3Path = parseS3Path(s3Uri);
        Path filePath = Paths.get(localPath);

        if (!Files.exists(filePath)) {
            return "Error: Local file not found: " + localPath;
        }

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Path.bucket)
                .key(s3Path.key)
                .build();

        s3Client.putObject(request, RequestBody.fromFile(filePath));

        return "Uploaded: " + localPath + " -> " + s3Uri;
    }

    private String downloadFile(String s3Uri, String localPath) throws Exception {
        S3Path s3Path = parseS3Path(s3Uri);
        Path filePath = Paths.get(localPath);

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(s3Path.bucket)
                .key(s3Path.key)
                .build();

        s3Client.getObject(request, filePath);

        return "Downloaded: " + s3Uri + " -> " + localPath;
    }

    private String copyS3Object(String sourceUri, String destUri) {
        S3Path source = parseS3Path(sourceUri);
        S3Path dest = parseS3Path(destUri);

        String copySource = source.bucket + "/" + source.key;

        CopyObjectRequest request = CopyObjectRequest.builder()
                .sourceBucket(source.bucket)
                .sourceKey(source.key)
                .destinationBucket(dest.bucket)
                .destinationKey(dest.key)
                .build();

        s3Client.copyObject(request);

        return "Copied: " + sourceUri + " -> " + destUri;
    }

    private S3Path parseS3Path(String path) {
        if (!path.startsWith("s3://")) {
            throw new IllegalArgumentException("S3 path must start with s3://");
        }

        String withoutProtocol = path.substring(5);
        int firstSlash = withoutProtocol.indexOf('/');

        S3Path result = new S3Path();

        if (firstSlash == -1) {
            result.bucket = withoutProtocol;
            result.key = "";
            result.prefix = "";
        } else {
            result.bucket = withoutProtocol.substring(0, firstSlash);
            result.key = withoutProtocol.substring(firstSlash + 1);
            result.prefix = result.key;
        }

        return result;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "iB";
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), pre);
    }

    private static class S3Path {
        String bucket;
        String key;
        String prefix;
    }
}
