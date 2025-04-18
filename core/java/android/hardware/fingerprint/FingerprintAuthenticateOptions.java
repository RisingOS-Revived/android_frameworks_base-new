/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.hardware.fingerprint;

import static android.hardware.fingerprint.FingerprintManager.SENSOR_ID_ANY;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.hardware.biometrics.AuthenticateOptions;
import android.hardware.biometrics.common.AuthenticateReason;
import android.os.Parcelable;

import com.android.internal.util.DataClass;

/**
 * Additional options when requesting Fingerprint authentication or detection.
 *
 * @hide
 */
@DataClass(
        genParcelable = true,
        genAidl = true,
        genBuilder = true,
        genSetters = true,
        genEqualsHashCode = true
)
public final class FingerprintAuthenticateOptions implements AuthenticateOptions, Parcelable {

    /** The user id for this operation. */
    private final int mUserId;
    private static int defaultUserId() {
        return 0;
    }

    /** The sensor id for this operation. */
    private int mSensorId;
    private static int defaultSensorId() {
        return SENSOR_ID_ANY;
    }

    /** If enrollment state should be ignored. */
    private final boolean mIgnoreEnrollmentState;
    private static boolean defaultIgnoreEnrollmentState() {
        return false;
    }

    /** The current doze state of the device. */
    @AuthenticateOptions.DisplayState
    private final int mDisplayState;
    private static int defaultDisplayState() {
        return DISPLAY_STATE_UNKNOWN;
    }

    /**
     * The package name for that operation that should be used for
     * {@link android.app.AppOpsManager} verification.
     *
     * This option may be overridden by the FingerprintManager using the caller's context.
     */
    @NonNull private String mOpPackageName;
    private static String defaultOpPackageName() {
        return "";
    }

    /**
     * The attribution tag, if any.
     *
     * This option may be overridden by the FingerprintManager using the caller's context.
     */
    @Nullable private String mAttributionTag;
    private static String defaultAttributionTag() {
        return null;
    }

    /**
     * The Vendor extension, if any.
     *
     * This option may be present when a vendor would like to send additional information for each
     * auth attempt.
     */
    @Nullable private AuthenticateReason.Vendor mVendorReason;
    private static AuthenticateReason.Vendor defaultVendorReason() {
        return null;
    }

    /**
     * If the authentication is requested due to mandatory biometrics being active.
     */
    private boolean mIsMandatoryBiometrics;

    // Code below generated by codegen v1.0.23.
    //
    // DO NOT MODIFY!
    // CHECKSTYLE:OFF Generated code
    //
    // To regenerate run:
    // $ codegen $ANDROID_BUILD_TOP/frameworks/base/core/java/android/hardware/fingerprint/FingerprintAuthenticateOptions.java
    //
    // To exclude the generated code from IntelliJ auto-formatting enable (one-time):
    //   Settings > Editor > Code Style > Formatter Control
    //@formatter:off


    @DataClass.Generated.Member
    /* package-private */ FingerprintAuthenticateOptions(
            int userId,
            int sensorId,
            boolean ignoreEnrollmentState,
            @AuthenticateOptions.DisplayState int displayState,
            @NonNull String opPackageName,
            @Nullable String attributionTag,
            @Nullable AuthenticateReason.Vendor vendorReason,
            boolean isMandatoryBiometrics) {
        this.mUserId = userId;
        this.mSensorId = sensorId;
        this.mIgnoreEnrollmentState = ignoreEnrollmentState;
        this.mDisplayState = displayState;
        com.android.internal.util.AnnotationValidations.validate(
                AuthenticateOptions.DisplayState.class, null, mDisplayState);
        this.mOpPackageName = opPackageName;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mOpPackageName);
        this.mAttributionTag = attributionTag;
        this.mVendorReason = vendorReason;
        this.mIsMandatoryBiometrics = isMandatoryBiometrics;

        // onConstructed(); // You can define this method to get a callback
    }

    /**
     * The user id for this operation.
     */
    @DataClass.Generated.Member
    public int getUserId() {
        return mUserId;
    }

    /**
     * The sensor id for this operation.
     */
    @DataClass.Generated.Member
    public int getSensorId() {
        return mSensorId;
    }

    /**
     * If enrollment state should be ignored.
     */
    @DataClass.Generated.Member
    public boolean isIgnoreEnrollmentState() {
        return mIgnoreEnrollmentState;
    }

    /**
     * The current doze state of the device.
     */
    @DataClass.Generated.Member
    public @AuthenticateOptions.DisplayState int getDisplayState() {
        return mDisplayState;
    }

    /**
     * The package name for that operation that should be used for
     * {@link android.app.AppOpsManager} verification.
     *
     * This option may be overridden by the FingerprintManager using the caller's context.
     */
    @DataClass.Generated.Member
    public @NonNull String getOpPackageName() {
        return mOpPackageName;
    }

    /**
     * The attribution tag, if any.
     *
     * This option may be overridden by the FingerprintManager using the caller's context.
     */
    @DataClass.Generated.Member
    public @Nullable String getAttributionTag() {
        return mAttributionTag;
    }

    /**
     * The Vendor extension, if any.
     *
     * This option may be present when a vendor would like to send additional information for each
     * auth attempt.
     */
    @DataClass.Generated.Member
    public @Nullable AuthenticateReason.Vendor getVendorReason() {
        return mVendorReason;
    }

    /**
     * If the authentication is requested due to mandatory biometrics being active.
     */
    @DataClass.Generated.Member
    public boolean isMandatoryBiometrics() {
        return mIsMandatoryBiometrics;
    }

    /**
     * The sensor id for this operation.
     */
    @DataClass.Generated.Member
    public @NonNull FingerprintAuthenticateOptions setSensorId( int value) {
        mSensorId = value;
        return this;
    }

    /**
     * The package name for that operation that should be used for
     * {@link android.app.AppOpsManager} verification.
     *
     * This option may be overridden by the FingerprintManager using the caller's context.
     */
    @DataClass.Generated.Member
    public @NonNull FingerprintAuthenticateOptions setOpPackageName(@NonNull String value) {
        mOpPackageName = value;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mOpPackageName);
        return this;
    }

    /**
     * The attribution tag, if any.
     *
     * This option may be overridden by the FingerprintManager using the caller's context.
     */
    @DataClass.Generated.Member
    public @NonNull FingerprintAuthenticateOptions setAttributionTag(@NonNull String value) {
        mAttributionTag = value;
        return this;
    }

    /**
     * The Vendor extension, if any.
     *
     * This option may be present when a vendor would like to send additional information for each
     * auth attempt.
     */
    @DataClass.Generated.Member
    public @NonNull FingerprintAuthenticateOptions setVendorReason(@NonNull AuthenticateReason.Vendor value) {
        mVendorReason = value;
        return this;
    }

    /**
     * If the authentication is requested due to mandatory biometrics being active.
     */
    @DataClass.Generated.Member
    public @NonNull FingerprintAuthenticateOptions setIsMandatoryBiometrics( boolean value) {
        mIsMandatoryBiometrics = value;
        return this;
    }

    @Override
    @DataClass.Generated.Member
    public boolean equals(@Nullable Object o) {
        // You can override field equality logic by defining either of the methods like:
        // boolean fieldNameEquals(FingerprintAuthenticateOptions other) { ... }
        // boolean fieldNameEquals(FieldType otherValue) { ... }

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        @SuppressWarnings("unchecked")
        FingerprintAuthenticateOptions that = (FingerprintAuthenticateOptions) o;
        //noinspection PointlessBooleanExpression
        return true
                && mUserId == that.mUserId
                && mSensorId == that.mSensorId
                && mIgnoreEnrollmentState == that.mIgnoreEnrollmentState
                && mDisplayState == that.mDisplayState
                && java.util.Objects.equals(mOpPackageName, that.mOpPackageName)
                && java.util.Objects.equals(mAttributionTag, that.mAttributionTag)
                && java.util.Objects.equals(mVendorReason, that.mVendorReason)
                && mIsMandatoryBiometrics == that.mIsMandatoryBiometrics;
    }

    @Override
    @DataClass.Generated.Member
    public int hashCode() {
        // You can override field hashCode logic by defining methods like:
        // int fieldNameHashCode() { ... }

        int _hash = 1;
        _hash = 31 * _hash + mUserId;
        _hash = 31 * _hash + mSensorId;
        _hash = 31 * _hash + Boolean.hashCode(mIgnoreEnrollmentState);
        _hash = 31 * _hash + mDisplayState;
        _hash = 31 * _hash + java.util.Objects.hashCode(mOpPackageName);
        _hash = 31 * _hash + java.util.Objects.hashCode(mAttributionTag);
        _hash = 31 * _hash + java.util.Objects.hashCode(mVendorReason);
        _hash = 31 * _hash + Boolean.hashCode(mIsMandatoryBiometrics);
        return _hash;
    }

    @Override
    @DataClass.Generated.Member
    public void writeToParcel(@NonNull android.os.Parcel dest, int flags) {
        // You can override field parcelling by defining methods like:
        // void parcelFieldName(Parcel dest, int flags) { ... }

        int flg = 0;
        if (mIgnoreEnrollmentState) flg |= 0x4;
        if (mIsMandatoryBiometrics) flg |= 0x80;
        if (mAttributionTag != null) flg |= 0x20;
        if (mVendorReason != null) flg |= 0x40;
        dest.writeInt(flg);
        dest.writeInt(mUserId);
        dest.writeInt(mSensorId);
        dest.writeInt(mDisplayState);
        dest.writeString(mOpPackageName);
        if (mAttributionTag != null) dest.writeString(mAttributionTag);
        if (mVendorReason != null) dest.writeTypedObject(mVendorReason, flags);
    }

    @Override
    @DataClass.Generated.Member
    public int describeContents() { return 0; }

    /** @hide */
    @SuppressWarnings({"unchecked", "RedundantCast"})
    @DataClass.Generated.Member
    /* package-private */ FingerprintAuthenticateOptions(@NonNull android.os.Parcel in) {
        // You can override field unparcelling by defining methods like:
        // static FieldType unparcelFieldName(Parcel in) { ... }

        int flg = in.readInt();
        boolean ignoreEnrollmentState = (flg & 0x4) != 0;
        boolean isMandatoryBiometrics = (flg & 0x80) != 0;
        int userId = in.readInt();
        int sensorId = in.readInt();
        int displayState = in.readInt();
        String opPackageName = in.readString();
        String attributionTag = (flg & 0x20) == 0 ? null : in.readString();
        AuthenticateReason.Vendor vendorReason = (flg & 0x40) == 0 ? null : (AuthenticateReason.Vendor) in.readTypedObject(AuthenticateReason.Vendor.CREATOR);

        this.mUserId = userId;
        this.mSensorId = sensorId;
        this.mIgnoreEnrollmentState = ignoreEnrollmentState;
        this.mDisplayState = displayState;
        com.android.internal.util.AnnotationValidations.validate(
                AuthenticateOptions.DisplayState.class, null, mDisplayState);
        this.mOpPackageName = opPackageName;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mOpPackageName);
        this.mAttributionTag = attributionTag;
        this.mVendorReason = vendorReason;
        this.mIsMandatoryBiometrics = isMandatoryBiometrics;

        // onConstructed(); // You can define this method to get a callback
    }

    @DataClass.Generated.Member
    public static final @NonNull Parcelable.Creator<FingerprintAuthenticateOptions> CREATOR
            = new Parcelable.Creator<FingerprintAuthenticateOptions>() {
        @Override
        public FingerprintAuthenticateOptions[] newArray(int size) {
            return new FingerprintAuthenticateOptions[size];
        }

        @Override
        public FingerprintAuthenticateOptions createFromParcel(@NonNull android.os.Parcel in) {
            return new FingerprintAuthenticateOptions(in);
        }
    };

    /**
     * A builder for {@link FingerprintAuthenticateOptions}
     */
    @SuppressWarnings("WeakerAccess")
    @DataClass.Generated.Member
    public static final class Builder {

        private int mUserId;
        private int mSensorId;
        private boolean mIgnoreEnrollmentState;
        private @AuthenticateOptions.DisplayState int mDisplayState;
        private @NonNull String mOpPackageName;
        private @Nullable String mAttributionTag;
        private @Nullable AuthenticateReason.Vendor mVendorReason;
        private boolean mIsMandatoryBiometrics;

        private long mBuilderFieldsSet = 0L;

        public Builder() {
        }

        /**
         * The user id for this operation.
         */
        @DataClass.Generated.Member
        public @NonNull Builder setUserId(int value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x1;
            mUserId = value;
            return this;
        }

        /**
         * The sensor id for this operation.
         */
        @DataClass.Generated.Member
        public @NonNull Builder setSensorId(int value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x2;
            mSensorId = value;
            return this;
        }

        /**
         * If enrollment state should be ignored.
         */
        @DataClass.Generated.Member
        public @NonNull Builder setIgnoreEnrollmentState(boolean value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x4;
            mIgnoreEnrollmentState = value;
            return this;
        }

        /**
         * The current doze state of the device.
         */
        @DataClass.Generated.Member
        public @NonNull Builder setDisplayState(@AuthenticateOptions.DisplayState int value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x8;
            mDisplayState = value;
            return this;
        }

        /**
         * The package name for that operation that should be used for
         * {@link android.app.AppOpsManager} verification.
         *
         * This option may be overridden by the FingerprintManager using the caller's context.
         */
        @DataClass.Generated.Member
        public @NonNull Builder setOpPackageName(@NonNull String value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x10;
            mOpPackageName = value;
            return this;
        }

        /**
         * The attribution tag, if any.
         *
         * This option may be overridden by the FingerprintManager using the caller's context.
         */
        @DataClass.Generated.Member
        public @NonNull Builder setAttributionTag(@NonNull String value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x20;
            mAttributionTag = value;
            return this;
        }

        /**
         * The Vendor extension, if any.
         *
         * This option may be present when a vendor would like to send additional information for each
         * auth attempt.
         */
        @DataClass.Generated.Member
        public @NonNull Builder setVendorReason(@NonNull AuthenticateReason.Vendor value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x40;
            mVendorReason = value;
            return this;
        }

        /**
         * If the authentication is requested due to mandatory biometrics being active.
         */
        @DataClass.Generated.Member
        public @NonNull Builder setIsMandatoryBiometrics(boolean value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x80;
            mIsMandatoryBiometrics = value;
            return this;
        }

        /** Builds the instance. This builder should not be touched after calling this! */
        public @NonNull FingerprintAuthenticateOptions build() {
            checkNotUsed();
            mBuilderFieldsSet |= 0x100; // Mark builder used

            if ((mBuilderFieldsSet & 0x1) == 0) {
                mUserId = defaultUserId();
            }
            if ((mBuilderFieldsSet & 0x2) == 0) {
                mSensorId = defaultSensorId();
            }
            if ((mBuilderFieldsSet & 0x4) == 0) {
                mIgnoreEnrollmentState = defaultIgnoreEnrollmentState();
            }
            if ((mBuilderFieldsSet & 0x8) == 0) {
                mDisplayState = defaultDisplayState();
            }
            if ((mBuilderFieldsSet & 0x10) == 0) {
                mOpPackageName = defaultOpPackageName();
            }
            if ((mBuilderFieldsSet & 0x20) == 0) {
                mAttributionTag = defaultAttributionTag();
            }
            if ((mBuilderFieldsSet & 0x40) == 0) {
                mVendorReason = defaultVendorReason();
            }
            FingerprintAuthenticateOptions o = new FingerprintAuthenticateOptions(
                    mUserId,
                    mSensorId,
                    mIgnoreEnrollmentState,
                    mDisplayState,
                    mOpPackageName,
                    mAttributionTag,
                    mVendorReason,
                    mIsMandatoryBiometrics);
            return o;
        }

        private void checkNotUsed() {
            if ((mBuilderFieldsSet & 0x100) != 0) {
                throw new IllegalStateException(
                        "This Builder should not be reused. Use a new Builder instance instead");
            }
        }
    }

    @DataClass.Generated(
            time = 1723436831455L,
            codegenVersion = "1.0.23",
            sourceFile = "frameworks/base/core/java/android/hardware/fingerprint/FingerprintAuthenticateOptions.java",
            inputSignatures = "private final  int mUserId\nprivate  int mSensorId\nprivate final  boolean mIgnoreEnrollmentState\nprivate final @android.hardware.biometrics.AuthenticateOptions.DisplayState int mDisplayState\nprivate @android.annotation.NonNull java.lang.String mOpPackageName\nprivate @android.annotation.Nullable java.lang.String mAttributionTag\nprivate @android.annotation.Nullable android.hardware.biometrics.common.AuthenticateReason.Vendor mVendorReason\nprivate  boolean mIsMandatoryBiometrics\nprivate static  int defaultUserId()\nprivate static  int defaultSensorId()\nprivate static  boolean defaultIgnoreEnrollmentState()\nprivate static  int defaultDisplayState()\nprivate static  java.lang.String defaultOpPackageName()\nprivate static  java.lang.String defaultAttributionTag()\nprivate static  android.hardware.biometrics.common.AuthenticateReason.Vendor defaultVendorReason()\nclass FingerprintAuthenticateOptions extends java.lang.Object implements [android.hardware.biometrics.AuthenticateOptions, android.os.Parcelable]\n@com.android.internal.util.DataClass(genParcelable=true, genAidl=true, genBuilder=true, genSetters=true, genEqualsHashCode=true)")
    @Deprecated
    private void __metadata() {}


    //@formatter:on
    // End of generated code

}
