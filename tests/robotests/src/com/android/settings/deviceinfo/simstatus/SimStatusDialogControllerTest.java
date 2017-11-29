/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.deviceinfo.simstatus;

import static com.android.settings.deviceinfo.simstatus.SimStatusDialogController
        .CELLULAR_NETWORK_TYPE_VALUE_ID;
import static com.android.settings.deviceinfo.simstatus.SimStatusDialogController
        .ICCID_INFO_LABEL_ID;
import static com.android.settings.deviceinfo.simstatus.SimStatusDialogController
        .ICCID_INFO_VALUE_ID;
import static com.android.settings.deviceinfo.simstatus.SimStatusDialogController
        .NETWORK_PROVIDER_VALUE_ID;
import static com.android.settings.deviceinfo.simstatus.SimStatusDialogController
        .OPERATOR_INFO_LABEL_ID;
import static com.android.settings.deviceinfo.simstatus.SimStatusDialogController
        .OPERATOR_INFO_VALUE_ID;
import static com.android.settings.deviceinfo.simstatus.SimStatusDialogController
        .PHONE_NUMBER_VALUE_ID;
import static com.android.settings.deviceinfo.simstatus.SimStatusDialogController
        .ROAMING_INFO_VALUE_ID;
import static com.android.settings.deviceinfo.simstatus.SimStatusDialogController
        .SERVICE_STATE_VALUE_ID;
import static com.android.settings.deviceinfo.simstatus.SimStatusDialogController
        .SIGNAL_STRENGTH_VALUE_ID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION_O)
public class SimStatusDialogControllerTest {

    @Mock
    private SimStatusDialogFragment mDialog;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private SubscriptionInfo mSubscriptionInfo;
    @Mock
    private ServiceState mServiceState;
    @Mock
    private PhoneStateListener mPhoneStateListener;
    @Mock
    private SignalStrength mSignalStrength;
    @Mock
    private CarrierConfigManager mCarrierConfigManager;
    @Mock
    private PersistableBundle mPersistableBundle;


    private SimStatusDialogController mController;
    private Context mContext;
    private Lifecycle mLifecycle;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        when(mDialog.getContext()).thenReturn(mContext);
        mLifecycle = new Lifecycle(() -> mLifecycle);
        mController = spy(
                new SimStatusDialogController(mDialog, mLifecycle, 0 /* phone id */));
        doReturn(mServiceState).when(mController).getCurrentServiceState();
        doReturn(0).when(mController).getDbm(any());
        doReturn(0).when(mController).getAsuLevel(any());
        doReturn(mPhoneStateListener).when(mController).getPhoneStateListener();
        doReturn("").when(mController).getPhoneNumber();
        doReturn(mSignalStrength).when(mController).getSignalStrength();
        ReflectionHelpers.setField(mController, "mTelephonyManager", mTelephonyManager);
        ReflectionHelpers.setField(mController, "mCarrierConfigManager", mCarrierConfigManager);
        ReflectionHelpers.setField(mController, "mSubscriptionInfo", mSubscriptionInfo);
        when(mCarrierConfigManager.getConfigForSubId(anyInt())).thenReturn(mPersistableBundle);
    }

    @Test
    public void initialize_updateNetworkProviderWithFoobarCarrier_shouldUpdateCarrierWithFoobar() {
        final String carrierName = "foobar";
        when(mServiceState.getOperatorAlphaLong()).thenReturn(carrierName);

        mController.initialize();

        verify(mDialog).setText(NETWORK_PROVIDER_VALUE_ID, carrierName);
    }

    @Test
    public void initialize_updatePhoneNumberWith1111111111_shouldUpdatePhoneNumber() {
        final String phoneNumber = "1111111111";
        doReturn(phoneNumber).when(mController).getPhoneNumber();

        mController.initialize();

        verify(mDialog).setText(PHONE_NUMBER_VALUE_ID, phoneNumber);
    }

    @Test
    public void initialize_updateLatestAreaInfoWithCdmaPhone_shouldRemoveOperatorInfoSetting() {
        when(mTelephonyManager.getPhoneType()).thenReturn(TelephonyManager.PHONE_TYPE_CDMA);

        mController.initialize();

        verify(mDialog).removeSettingFromScreen(OPERATOR_INFO_LABEL_ID);
        verify(mDialog).removeSettingFromScreen(OPERATOR_INFO_VALUE_ID);
    }

    @Test
    public void initialize_updateServiceStateWithInService_shouldUpdateTextToBeCInService() {
        when(mServiceState.getState()).thenReturn(ServiceState.STATE_IN_SERVICE);

        mController.initialize();

        final String inServiceText = mContext.getResources().getString(
                R.string.radioInfo_service_in);
        verify(mDialog).setText(SERVICE_STATE_VALUE_ID, inServiceText);
    }

    @Test
    public void initialize_updateDataStateWithPowerOff_shouldUpdateSettingAndResetSignalStrength() {
        when(mServiceState.getState()).thenReturn(ServiceState.STATE_POWER_OFF);

        mController.initialize();

        final String offServiceText = mContext.getResources().getString(
                R.string.radioInfo_service_off);
        verify(mDialog).setText(SERVICE_STATE_VALUE_ID, offServiceText);
        verify(mDialog).setText(SIGNAL_STRENGTH_VALUE_ID, "0");
    }

    @Test
    public void initialize_updateSignalStrengthWith50_shouldUpdateSignalStrengthTo50() {
        final int signalDbm = 50;
        final int signalAsu = 50;
        doReturn(signalDbm).when(mController).getDbm(mSignalStrength);
        doReturn(signalAsu).when(mController).getAsuLevel(mSignalStrength);

        mController.initialize();

        final String signalStrengthString = mContext.getResources().getString(
                R.string.sim_signal_strength, signalDbm, signalAsu);
        verify(mDialog).setText(SIGNAL_STRENGTH_VALUE_ID, signalStrengthString);
    }

    @Test
    public void initialize_updateNetworkTypeWithEdge_shouldUpdateSettingToEdge() {
        when(mTelephonyManager.getDataNetworkType(anyInt())).thenReturn(
                TelephonyManager.NETWORK_TYPE_EDGE);

        mController.initialize();

        verify(mDialog).setText(CELLULAR_NETWORK_TYPE_VALUE_ID,
                TelephonyManager.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_EDGE));
    }

    @Test
    public void initialize_updateRoamingStatusIsRoaming_shouldSetSettingToRoaming() {
        when(mServiceState.getRoaming()).thenReturn(true);

        mController.initialize();

        final String roamingOnString = mContext.getResources().getString(
                R.string.radioInfo_roaming_in);
        verify(mDialog).setText(ROAMING_INFO_VALUE_ID, roamingOnString);
    }

    @Test
    public void initialize_updateRoamingStatusNotRoaming_shouldSetSettingToRoamingOff() {
        when(mServiceState.getRoaming()).thenReturn(false);

        mController.initialize();

        final String roamingOffString = mContext.getResources().getString(
                R.string.radioInfo_roaming_not);
        verify(mDialog).setText(ROAMING_INFO_VALUE_ID, roamingOffString);
    }

    @Test
    public void initialize_doNotShowIccid_shouldRemoveIccidSetting() {
        when(mPersistableBundle.getBoolean(
                CarrierConfigManager.KEY_SHOW_ICCID_IN_SIM_STATUS_BOOL)).thenReturn(
                false);

        mController.initialize();

        verify(mDialog).removeSettingFromScreen(ICCID_INFO_LABEL_ID);
        verify(mDialog).removeSettingFromScreen(ICCID_INFO_VALUE_ID);
    }

    @Test
    public void initialize_showIccid_shouldSetIccidToSetting() {
        final String iccid = "12351351231241";
        when(mPersistableBundle.getBoolean(
                CarrierConfigManager.KEY_SHOW_ICCID_IN_SIM_STATUS_BOOL)).thenReturn(
                true);
        doReturn(iccid).when(mController).getSimSerialNumber(anyInt());

        mController.initialize();

        verify(mDialog).setText(ICCID_INFO_VALUE_ID, iccid);
    }
}
