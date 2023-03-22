package org.touchhome.bundle.gdrive;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.primitives.Bytes;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.touchhome.bundle.api.fs.FileSystemProvider;
import org.touchhome.bundle.api.fs.TreeNode;

public class GDriveFileSystem implements FileSystemProvider {

  public static final File ROOT = new File().setId("root").setName("root").setMimeType("application/vnd.google-apps.folder")
      .setModifiedTime(new DateTime(0));
  // private static final String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";
  // private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private final LoadingCache<String, List<File>> fileCache;
  // private GoogleAuthorizationCodeFlow flow;
  private Drive drive;
  private About about;
  private long aboutLastAccess;
  private GDriveEntity entity;

  private long connectionHashCode;

  public GDriveFileSystem(GDriveEntity entity) {
    this.entity = entity;

    this.fileCache = CacheBuilder.newBuilder().
        expireAfterWrite(1, TimeUnit.HOURS).build(new CacheLoader<>() {
          public List<File> load(@NotNull String id) {
            try {
              return Collections.singletonList(getDrive().files().get(id).setFields("*").execute());
            } catch (Exception ex) {
              try {
                return getDrive().files().list().setQ("name = '" + id + "'").setFields("*").execute().getFiles();
              } catch (Exception ignore) {
              }
              return Collections.emptyList();
            }
          }
        });
  }

    /*@SneakyThrows
    public Credential buildDriveByCode(String code) {
        drive = null;
        flow = null;
        Credential credentials = exchangeCode(code);
        drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credentials).setApplicationName(APPLICATION_NAME).build();
        return credentials;
    }*/

    /*Credential getStoredCredentials() {
        GoogleCredential credential = new GoogleCredential.Builder()
                .setJsonFactory(JSON_FACTORY)
                .setTransport(HTTP_TRANSPORT)
                .setClientSecrets(entity.getGoogleClientID(), entity.getGoogleClientSecret().asString()).build();
        credential.setAccessToken(entity.getGoogleAccessToken());
        credential.setRefreshToken(entity.getGoogleRefreshToken());
        return credential;
    }*/

  /* */

  /**
   * Build an authorization flow and store it as a static class attribute.
   *
   * @return GoogleAuthorizationCodeFlow instance.
   *//*
    GoogleAuthorizationCodeFlow getFlow() {
        if (flow == null) {
            GoogleClientSecrets googleClientSecrets = new GoogleClientSecrets();
            GoogleClientSecrets.Details det = new GoogleClientSecrets.Details();
            det.setClientId(entity.getGoogleClientID());
            det.setClientSecret(entity.getGoogleClientSecret().asString());
            det.setRedirectUris(Collections.singletonList(REDIRECT_URI));
            googleClientSecrets.setInstalled(det);

            flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, googleClientSecrets,
                    SCOPES).setAccessType("offline").setApprovalPrompt("force").build();
        }
        return flow;
    }*/

  /* /**
   * Exchange an authorization code for OAuth 2.0 credentials.
   *
   * @param authorizationCode Authorization code to exchange for OAuth 2.0
   *                          credentials.
   * @return OAuth 2.0 credentials.
   */
 /*   Credential exchangeCode(String authorizationCode) throws IOException {
        GoogleTokenResponse response =
                getFlow().newTokenRequest(authorizationCode).setRedirectUri(REDIRECT_URI).setScopes(SCOPES).execute();
        return getFlow().createAndStoreCredential(response, null);
    }*/

    /*public String getAuthorizationUrl() {
        return getFlow().newAuthorizationUrl().setRedirectUri(REDIRECT_URI)
                .setClientId(entity.getGoogleClientID())
                .set("user_id", entity.getEmail()).build();
    }*/
  @SneakyThrows
  public About getAbout() {
    // not often that once per minute
    if (about == null || System.currentTimeMillis() - aboutLastAccess > 600000) {
      about = getDrive().about().get().setFields("*").execute();
      aboutLastAccess = System.currentTimeMillis();
    }
    return about;
  }

  public void dispose() {
    this.drive = null;
    // this.flow = null;
        /* this.setRoot(new GoogleDriveCacheFileSystem(new GDriveFile(
                new File().setId("root").setName("root").setMimeType("application/vnd.google-apps.folder")
                        .setModifiedTime(new DateTime(0))), null)); */
  }

  @Override
  public boolean restart(boolean force) {
    try {
      if (!force && connectionHashCode == entity.getConnectionHashCode()) {
        return true;
      }
      dispose();
      getChildren("");
      entity.setStatusOnline();
      connectionHashCode = entity.getConnectionHashCode();
      return true;
    } catch (Exception ex) {
      entity.setStatusError(ex);
      return false;
    }
  }

  @Override
  public void setEntity(Object entity) {
    this.entity = (GDriveEntity) entity;
    restart(false);
  }

  @Override
  public long getTotalSpace() {
    return getAbout().getStorageQuota().getLimit();
  }

  @Override
  public long getUsedSpace() {
    return getAbout().getStorageQuota().getUsageInDrive();
  }

  @Override
  @SneakyThrows
  public InputStream getEntryInputStream(@NotNull String id) {
    try (InputStream stream = getDrive().files().get(id).executeMediaAsInputStream()) {
      return new ByteArrayInputStream(IOUtils.toByteArray(stream));
    }
  }

  private TreeNode buildTreeNode(File file) {
    boolean isDirectory = file.getMimeType().equals("application/vnd.google-apps.folder");
    boolean hasChildren = true;
    return new TreeNode(isDirectory, isDirectory && !hasChildren, file.getName(), file.getId(),
        file.getSize(), file.getModifiedTime().getValue(), this, file.getMimeType());
  }

  @Override
  @SneakyThrows
  public Set<TreeNode> toTreeNodes(Set<String> ids) {
    Set<TreeNode> fmPaths = new HashSet<>();
    for (String id : ids) {
      for (File file : fileCache.get(id)) {
        fmPaths.add(buildTreeNode(file));
      }
    }
    return fmPaths;
  }

  @Override
  @SneakyThrows
  public TreeNode delete(@NotNull Set<String> ids) {
    List<File> files = new ArrayList<>();
    for (String id : ids) {
      for (File file : fileCache.get(id)) {
        getDrive().files().delete(file.getId()).execute();
        clearCache(file);
        files.add(file);
      }
    }
    return buildRoot(files);
  }

  private TreeNode buildRoot(Collection<File> files) {
    TreeNode rootPath = this.buildTreeNode(ROOT);
    for (File file : files) {
      TreeNode child = buildTreeNode(file);
      buildParents(rootPath, child, file.getParents());
    }
    return rootPath;
  }

  @SneakyThrows
  private void buildParents(TreeNode rootPath, TreeNode child, List<String> parents) {
    if (parents != null) {
      for (String parent : parents) {
        List<File> files = fileCache.get(parent);
        if (!files.isEmpty()) {
          File file = files.iterator().next();
          TreeNode treeNode = buildTreeNode(file);
          treeNode.addChild(child);
          buildParents(rootPath, treeNode, file.getParents());
        }
      }
    } else {
      rootPath.addChild(child);
    }
  }

  @Override
  @SneakyThrows
  public TreeNode create(@NotNull String parentId, @NotNull String name, boolean isDir, UploadOption uploadOption) {
    File existedFile = findFileByNameAndParent(name, parentId);

    if (uploadOption != UploadOption.Replace) {
      if (existedFile != null) {
        if (uploadOption == UploadOption.SkipExist) {
          return null;
        } else if (uploadOption == UploadOption.Error) {
          throw new FileAlreadyExistsException("File " + name + " already exists");
        }
      }
    }

    this.fileCache.invalidate(name);

    File file = new File().setParents(Collections.singletonList(parentId)).setName(name);
    file.setModifiedTime(new DateTime(System.currentTimeMillis()));
    if (isDir) {
      file.setMimeType("application/vnd.google-apps.folder");
    }

    file = getDrive().files().create(file).setFields("*").execute();
    this.fileCache.put(file.getId(), Collections.singletonList(file));
    return buildRoot(Collections.singleton(file));
  }

  @Override
  @SneakyThrows
  public TreeNode rename(@NotNull String id, @NotNull String newName, UploadOption uploadOption) {
    List<File> files = fileCache.get(id);
    if (files.size() == 1) {
      File file = files.get(0);
      clearCache(file);
      file.setName(newName);

      if (uploadOption != UploadOption.Replace) {
        File existedFile =
            Optional.ofNullable(fileCache.get(newName)).map(files1 -> files1.isEmpty() ? null : files1.get(0))
                .orElse(null);
        if (existedFile != null) {
          if (uploadOption == UploadOption.SkipExist) {
            return null;
          } else if (uploadOption == UploadOption.Error) {
            throw new FileAlreadyExistsException("File " + newName + " already exists");
          }
        }
      }

      file = getDrive().files().update(file.getId(), file).setFileId("*").execute();
      this.fileCache.put(file.getId(), Collections.singletonList(file));

      return buildRoot(Collections.singleton(file));
    }
    throw new IllegalStateException("File '" + id + "' not found");
  }

  private void clearCache(File file) {
    this.fileCache.invalidate(file.getId());
    this.fileCache.invalidate(file.getName());
  }

  @Override
  @SneakyThrows
  public Set<TreeNode> getChildrenRecursively(@NotNull String parentId) {
    List<File> files = queryFiles("trashed = false");
    TreeNode root = new TreeNode();
    Map<String, Pair<TreeNode, File>> idToObject = new HashMap<>();
    for (File file : files) {
      idToObject.put(file.getId(), Pair.of(root.addChild(buildTreeNode(file)), file));
    }
    for (Pair<TreeNode, File> pair : idToObject.values()) {
      File file = pair.getValue();
      if (file.getParents().isEmpty()) {
        root.addChild(pair.getKey());
      } else {
        for (String parent : file.getParents()) {
          Pair<TreeNode, File> parentObject = idToObject.get(parent);
          if (parentObject != null) {
            parentObject.getKey().addChild(pair.getKey());
          }
        }
      }
    }
    return root.getChildren();
  }

  @Override
  @SneakyThrows
  // TODO: not optimised
  public Set<TreeNode> loadTreeUpToChild(@Nullable String rootPath, @NotNull String id) {
    return getChildrenRecursively("");
  }

  @Override
  @SneakyThrows
  public Set<TreeNode> getChildren(@NotNull String parentId) {
    String query = "('root' in parents or sharedWithMe = true) and trashed = false";
    if (!StringUtils.isEmpty(parentId)) {
      query = "'" + parentId + "' in parents";
    }
    List<File> files = queryFiles(query);
    return files.stream().map(this::buildTreeNode).collect(Collectors.toSet());
  }

  @NotNull
  private List<File> queryFiles(String query) throws IOException {
    String pageToken = null;
    List<File> files = new ArrayList<>();
    do {
      FileList result = getDrive().files().list()
          .setQ(query)
          .setFields("nextPageToken, files(id, parents, name, mimeType, modifiedTime, size)")
          .setPageToken(pageToken)
          .execute();
      files.addAll(result.getFiles());
      pageToken = result.getNextPageToken();
    } while (pageToken != null);
    for (File file : files) {
      fileCache.put(file.getId(), Collections.singletonList(file));
    }
    return files;
  }

  @Override
  public TreeNode copy(@NotNull Collection<TreeNode> entries, @NotNull String targetId, UploadOption uploadOption) {
    List<File> result = new ArrayList<>();
    this.fileCache.invalidateAll();
    copyEntries(entries, targetId, uploadOption, result);
    return buildRoot(result);
  }

  @SneakyThrows
  private void copyEntries(Collection<TreeNode> entries, String targetId, UploadOption uploadOption, List<File> result) {
    for (TreeNode entry : entries) {

      // if targetFile is folder than it's regular copy
      List<File> targetFiles = fileCache.get(targetId);
      if (targetFiles.size() != 1) {
        throw new RuntimeException("Unable to find GDrive target: " + targetId);
      }
      File targetFileOrFolder = targetFiles.get(0);

      if (targetFileOrFolder != null && !isFile(targetFileOrFolder) && !entry.getAttributes().isDir()) {
        // entry.getId() is actually a file name, not GDrive id
        File file = findFileByNameAndParent(entry.getId(), targetFileOrFolder.getId());
        try (InputStream stream = entry.getInputStream()) {
          byte[] content = IOUtils.toByteArray(stream);
          if (file != null) {
            if (uploadOption == UploadOption.Append) {
              content = Bytes.concat(
                  IOUtils.toByteArray(getDrive().files().get(entry.getId()).executeMediaAsInputStream()),
                  content);
            }
            result.add(addToCache(getDrive().files().update(file.getId(), new File(),
                new ByteArrayContent(null, content)).setFields("*").execute()));
          } else {
            File gDriveFile = new File();
            gDriveFile.setName(entry.getName());
            gDriveFile.setParents(Collections.singletonList(targetId));
            result.add(addToCache(getDrive().files().create(gDriveFile, new ByteArrayContent(null, content))
                .setFields("*").execute()));
          }
        }
      } else {
        // if (!optionalFile.isPresent()) {
        File gDriveFile = new File();
        gDriveFile.setName(entry.getName());
        gDriveFile.setParents(Collections.singletonList(targetId));
        gDriveFile.setMimeType("application/vnd.google-apps.folder");
        gDriveFile.setModifiedTime(new DateTime(System.currentTimeMillis()));
        File newFolder = addToCache(getDrive().files().create(gDriveFile).setFields("*").execute());
        String parentTargetId = newFolder.getId();
        result.add(newFolder);
        // }
        copyEntries(entry.getFileSystem().getChildren(entry), parentTargetId, uploadOption, result);
      }
    }
  }

  private File addToCache(File file) {
    this.fileCache.put(file.getId(), Collections.singletonList(file));
    return file;
  }

  @Nullable
  private File findFileByNameAndParent(String fileIdOrName, String targetFileOrFolder) throws ExecutionException {
    List<File> files = this.fileCache.get(fileIdOrName);
    return files.stream().filter(f -> f.getParents().contains(targetFileOrFolder))
        .findAny().orElse(null);
  }

  private boolean isFile(File file) {
    return file != null && !file.getMimeType().equals("application/vnd.google-apps.folder");
  }

  @SneakyThrows
  private Drive getDrive() {
    if (this.drive == null) {
      ByteArrayInputStream stream = new ByteArrayInputStream(entity.getCredentials().getBytes());
      GoogleCredential credential = GoogleCredential.fromStream(stream, HTTP_TRANSPORT, JSON_FACTORY)
          .createScoped(Collections.singletonList(DriveScopes.DRIVE));
            /*GoogleCredential credential = new GoogleCredential.Builder()
                    .setTransport(HTTP_TRANSPORT)
                    .setJsonFactory(JSON_FACTORY)
                    .setServiceAccountId(credentials.getAccount())
                    .setServiceAccountScopes(Collections.singletonList(DriveScopes.DRIVE))
                    .setServiceAccountUser(entity.getEmail())
                    .setServiceAccountPrivateKey(credentials.getPrivateKey())
                    .setServiceAccountPrivateKeyId(credentials.getPrivateKeyId())
                    .setServiceAccountProjectId(credentials.getProjectId())
                    .build();*/
      this.drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
          .setApplicationName("touchhome").build();

           /* this.drive =
                    new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getStoredCredentials()).setApplicationName(APPLICATION_NAME)
                            .build();*/
    }
    return this.drive;
  }
}
