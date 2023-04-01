package org.homio.bundle.gdrive;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.homio.bundle.api.BundleEntrypoint;

@Log4j2
@Component
@RequiredArgsConstructor
public class GDriveEntrypoint implements BundleEntrypoint {

  @Override
  public void init() {
  }

  @Override
  public int order() {
    return 2100;
  }

  @Override
  public BundleImageColorIndex getBundleImageColorIndex() {
    return BundleImageColorIndex.ONE;
  }
}
