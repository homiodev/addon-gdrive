package org.homio.addon.gdrive;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonObjectParser;
import jakarta.persistence.Entity;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.homio.api.Context;
import org.homio.api.entity.storage.BaseFileSystemEntity;
import org.homio.api.entity.types.StorageEntity;
import org.homio.api.exception.NotFoundException;
import org.homio.api.model.ActionResponseModel;
import org.homio.api.ui.UISidebarChildren;
import org.homio.api.ui.field.UIField;
import org.homio.api.ui.field.action.UIContextMenuUploadAction;
import org.homio.api.ui.field.action.v1.UIInputBuilder;
import org.homio.api.util.Lang;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.homio.addon.gdrive.GDriveFileSystem.JSON_FACTORY;

@SuppressWarnings("unused")
@Entity
@UISidebarChildren(icon = "fab fa-google-drive", color = "#0DA10A")
public class GDriveEntity extends StorageEntity implements BaseFileSystemEntity<GDriveFileSystem> {

  @Override
  public @NotNull String getFileSystemRoot() {
    return "/";
  }

  @Override
  public @NotNull String getFileSystemAlias() {
    return "GDRIVE";
  }

  @Override
  public boolean isShowInFileManager() {
    return true;
  }

  @Override
  public String getFileSystemIcon() {
    return "fab fa-google-drive";
  }

  @Override
  public String getFileSystemIconColor() {
    return "#90D80A";
  }

  @Override
  public String getDescriptionImpl() {
    if (!isHasCredentials()) {
      return Lang.getServerMessage("GDRIVE_DESCRIPTION");
    }
    return null;
  }

  @UIField(order = 25, hideOnEmpty = true, hideInEdit = true)
  public String getEmail() {
    try {
      return isHasCredentials() ? new JSONObject(getCredentials()).getString("client_email") : "";
    } catch (Exception ex) {
      return ex.getMessage();
    }
  }

  @UIField(order = 26, hideOnEmpty = true, hideInEdit = true)
  public String getProjectId() {
    try {
      return isHasCredentials() ? new JSONObject(getCredentials()).getString("project_id") : "";
    } catch (Exception ex) {
      return ex.getMessage();
    }
  }

  @UIField(order = 30, hideInEdit = true)
  public boolean isHasCredentials() {
    return getJsonData().has("credentials");
  }

  @JsonIgnore
  public String getCredentials() {
    if (isHasCredentials()) {
      return getJsonData("credentials");
    }
    throw new NotFoundException("Unable to find saved service credentials");
  }

  @SneakyThrows
  @UIContextMenuUploadAction(value = "UPLOAD_CREDENTIALS", icon = "fas fa-upload",
    supportedFormats = {MediaType.APPLICATION_JSON_VALUE})
  public ActionResponseModel uploadCredentials(Context context, JSONObject params) {
    MultipartFile file = ((MultipartFile[]) params.get("files"))[0];
    String credentials = IOUtils.toString(file.getInputStream(), StandardCharsets.UTF_8);
    JsonObjectParser parser = new JsonObjectParser(JSON_FACTORY);
    GenericJson fileContents =
      parser.parseAndClose(new ByteArrayInputStream(credentials.getBytes()), StandardCharsets.UTF_8, GenericJson.class);
    String fileType = (String) fileContents.get("type");
    if (fileType == null) {
      throw new IOException("Error reading credentials from stream, 'type' field not specified.");
    }
    setJsonData("credentials", credentials);
    context.db().save(this);
    return ActionResponseModel.showSuccess("ACTION.SUCCESS");
  }

    /* public GDriveEntity setEmail(String value) {
        return setJsonData("email", value);
    }

    @UIField(order = 30, required = true, inlineEditWhenEmpty = true)
    public String getGoogleClientID() {
        return getJsonData("clientID");
    }

    public GDriveEntity setGoogleClientID(String value) {
        return setJsonData("clientID", value);
    }

    @UIField(order = 40, required = true, inlineEditWhenEmpty = true)
    public SecureString getGoogleClientSecret() {
        return getJsonSecure("clientSecret");
    }

    public GDriveEntity setGoogleClientSecret(String value) {
        return setJsonDataSecure("clientSecret", value);
    }

    @UIField(order = 50, hideInEdit = true)
    public String getGoogleAccessToken() {
        return getJsonData("accessToken");
    }

    public GDriveEntity setGoogleAccessToken(String value) {
        return setJsonData("accessToken", value);
    }

    @UIField(order = 60, hideInEdit = true)
    public String getGoogleRefreshToken() {
        return getJsonData("refreshToken");
    }

    public GDriveEntity setGoogleRefreshToken(String value) {
        return setJsonData("refreshToken", value);
    }*/

  @Override
  protected @NotNull String getDevicePrefix() {
    return "gdrive";
  }

  @Override
  public boolean requireConfigure() {
    return !isHasCredentials();
    // return StringUtils.isEmpty(getGoogleAccessToken());
  }

  @Override
  public @NotNull GDriveFileSystem buildFileSystem(@NotNull Context context, int alias) {
    return new GDriveFileSystem(this);
  }

  @Override
  public long getConnectionHashCode() {
    return 0L;
  }

  @Override
  public boolean isShowHiddenFiles() {
    return true;
  }

  @Override
  public String getDefaultName() {
    return "GDrive";
  }

  @Override
  public void assembleActions(UIInputBuilder uiInputBuilder) {
        /*if (StringUtils.isNotEmpty(getGoogleAccessToken())) {
            return;
        }
        GDriveFileSystem gDriveFileSystem = this.getFileSystem(uiInputBuilder.getEntityContext());
        uiInputBuilder.addOpenDialogSelectableButton("OAUTH2_AUTHENTICATE", "fas fa-sign-in-alt", null,
                null, (entityContext, params) -> {
                    String code = params.getString("code");

                    try {
                        Credential credential = gDriveFileSystem.buildDriveByCode(code);
                        entityContext.save(this
                                .setStatus(Status.ONLINE)
                                .setStatusMessage(null)
                                .setGoogleAccessToken(credential.getAccessToken())
                                .setGoogleRefreshToken(credential.getRefreshToken()));
                        entityContext.ui().sendSuccessMessage("GDrive Oauth2 authenticate successful");
                    } catch (Exception ex) {
                        String msg = homioUtils.getErrorMessage(ex);
                        entityContext.save(setStatus(Status.ERROR).setStatusMessage(msg));
                        entityContext.ui().sendErrorMessage("Error during Oauth2 authenticate. " + msg);
                    }
                    return null;
                }).editDialog(dialogBuilder ->
                dialogBuilder.addTextInput("code", "past_code_here", true).edit(builder ->
                        builder.setDescription(Lang.getServerMessage("gdrive.code_description", "URL",
                                gDriveFileSystem.getAuthorizationUrl())))); */
  }
}
