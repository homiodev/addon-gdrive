package org.homio.bundle.gdrive;

import lombok.Getter;
import org.springframework.stereotype.Component;
import org.homio.bundle.api.EntityContext;
import org.homio.bundle.api.entity.storage.Scratch3BaseFileSystemExtensionBlocks;

@Getter
@Component
public class Scratch3GDriveBlocks extends Scratch3BaseFileSystemExtensionBlocks<GDriveEntrypoint, GDriveEntity> {

  public Scratch3GDriveBlocks(EntityContext entityContext, GDriveEntrypoint gDriveEntrypoint) {
    super("#51633C", entityContext, gDriveEntrypoint, GDriveEntity.class);
  }
}
