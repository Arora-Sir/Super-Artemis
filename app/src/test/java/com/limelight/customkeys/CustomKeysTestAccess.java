package com.limelight.customkeys;

import java.util.List;

// Opens the package-private custom key parser to tests in other packages
public final class CustomKeysTestAccess {
    private CustomKeysTestAccess() {
    }

    public static List<CustomKey> parse(String json) throws CustomKeyStore.FormatException {
        return CustomKeyStore.parse(json);
    }
}
