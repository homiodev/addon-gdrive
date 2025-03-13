package org.homio.addon.gdrive;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.homio.api.AddonConfiguration;
import org.homio.api.AddonEntrypoint;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@AddonConfiguration
@RequiredArgsConstructor
public class GDriveEntrypoint implements AddonEntrypoint {

  @Override
  public void init() {
  }

  @Override
  public @NotNull AddonImageColorIndex getAddonImageColorIndex() {
    return AddonImageColorIndex.ONE;
  }
}
