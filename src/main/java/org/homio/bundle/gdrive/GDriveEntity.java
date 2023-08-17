package org.homio.bundle.gdrive;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.nio.charset.StandardCharsets;
import javax.persistence.Entity;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.homio.bundle.api.EntityContext;
import org.homio.bundle.api.entity.storage.BaseFileSystemEntity;
import org.homio.bundle.api.entity.types.StorageEntity;
import org.homio.bundle.api.exception.NotFoundException;
import org.homio.bundle.api.model.ActionResponseModel;
import org.homio.bundle.api.ui.UISidebarChildren;
import org.homio.bundle.api.ui.field.UIField;
import org.homio.bundle.api.ui.field.action.UIContextMenuUploadAction;
import org.homio.bundle.api.ui.field.action.v1.UIInputBuilder;

@Entity
@UISidebarChildren(icon = "fab fa-google-drive", color = "#0DA10A")
public class GDriveEntity extends StorageEntity<GDriveEntity> implements BaseFileSystemEntity<GDriveEntity, GDriveFileSystem> {

  public static final String PREFIX = "gdrive_";

  @Override
  public String getFileSystemAlias() {
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

  @UIField(order = 25, hideInEdit = true, copyButton = true)
  public String getEmail() {
    return isHasCredentials() ? new JSONObject(getCredentials()).getString("client_email") : "NOT_FOUND";
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
  public ActionResponseModel uploadCredentials(EntityContext entityContext, JSONObject params) {
    MultipartFile file = ((MultipartFile[]) params.get("files"))[0];
    String credentials = IOUtils.toString(file.getInputStream(), StandardCharsets.UTF_8);
    setJsonData("credentials", credentials);
    if (new GDriveFileSystem(GDriveEntity.this).restart(true)) {
      entityContext.save(this);
      return ActionResponseModel.showSuccess("ACTION.SUCCESS");
    }
    return ActionResponseModel.showError(this.getStatusMessage());
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
  public String getEntityPrefix() {
    return PREFIX;
  }

  @Override
  public boolean requireConfigure() {
    return !isHasCredentials();
    // return StringUtils.isEmpty(getGoogleAccessToken());
  }

  @Override
  public GDriveFileSystem buildFileSystem(EntityContext entityContext) {
    return new GDriveFileSystem(this);
  }

  @Override
  public long getConnectionHashCode() {
    return 0L;
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
