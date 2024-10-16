LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := android.hardware.biometrics.fingerprint@2.1-service.sony
LOCAL_INIT_RC := android.hardware.biometrics.fingerprint@2.1-service.sony.rc
LOCAL_VINTF_FRAGMENTS := android.hardware.biometrics.fingerprint@2.1-service.sony.xml
LOCAL_PROPRIETARY_MODULE := true
LOCAL_MODULE_RELATIVE_PATH := hw
LOCAL_SRC_FILES := \
    $(call all-subdir-cpp-files) \
    QSEEComFunc.c \
    common.c

LOCAL_CFLAGS += \
    -DFINGERPRINT_TYPE_EGISTEC \
    -DEGIS_QSEE_APP_PATH=\"/vendor/firmware\" \
    -DEGISTEC_SAVE_TEMPLATE_RETURNS_SIZE \
    -DEGIS_QSEE_APP_NAME=\"egista\"

LOCAL_SHARED_LIBRARIES := \
    android.hardware.biometrics.fingerprint@2.1 \
    libcutils \
    libdl \
    libdmabufheap \
    libhardware \
    libhidlbase \
    liblog \
    libutils

ifeq ($(TARGET_COMPILE_WITH_MSM_KERNEL),true)
LOCAL_C_INCLUDES += $(TARGET_OUT_INTERMEDIATES)/KERNEL_OBJ/usr/include
LOCAL_ADDITIONAL_DEPENDENCIES := $(TARGET_OUT_INTERMEDIATES)/KERNEL_OBJ/usr
endif

LOCAL_CFLAGS += \
    -DPLATFORM_SDK_VERSION=$(PLATFORM_SDK_VERSION) \
    -fexceptions

include $(BUILD_EXECUTABLE)
