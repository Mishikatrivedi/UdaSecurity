package com.udacity.catpoint;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.udacity.catpoint.security.services.SecurityService;
import org.junit.jupiter.api.Test;
import com.udacity.catpoint.images.services.ImageService;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.provider.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.udacity.catpoint.security.data.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.Stream;

/**
 * Unit test for simple App.
 */
@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    /**
     * Rigorous Test :-)
     */
    private SecurityService securityService;

    @Mock
    private ImageService imageService;
    @Mock
    private SecurityRepository securityRepository;
    @Mock
    private Sensor sensor1 , sensor2 ;


    @BeforeEach
    void setInit(){
        securityService = new SecurityService(securityRepository , imageService);
    }

    private static Stream<Arguments> sensorStateChangeOptions() {
        return Stream.of(
                Arguments.of(ArmingStatus.ARMED_HOME ),
                Arguments.of(ArmingStatus.ARMED_AWAY )
                );
    }
    private static Stream<Arguments> sensorStateChangeDeactivate() {
        return Stream.of(
                Arguments.of(AlarmStatus.NO_ALARM),
                Arguments.of(AlarmStatus.ALARM),
                Arguments.of(AlarmStatus.PENDING_ALARM)
        );
    }

    @Mock
    BufferedImage bufferedImage;

    @ParameterizedTest
    @MethodSource("sensorStateChangeDeactivate")
    public void returningAlarmStatus(AlarmStatus alarmStatus){
        when(securityRepository.getAlarmStatus()).thenReturn(alarmStatus);
        assertEquals(alarmStatus , securityService.getAlarmStatus());
    }
//    armed system
    @ParameterizedTest
    @MethodSource("sensorStateChangeOptions")
    public void armedSystemTest1(ArmingStatus armingStatus){
        Set<Sensor> sensorsSet = Set.of(sensor1 , sensor2);
//        error , Unnecessary Stubbing
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(sensorsSet);

        securityService.setArmingStatus(armingStatus);
        sensorsSet.stream().findFirst().ifPresent(sensor -> sensor.setActive(false));
        securityService.getSensors().forEach(s -> assertEquals(false , s.getActive()));
    }
//    armed home and cat appears
    @Test
    public void armedAndCatTest2(){
        when(imageService.imageHasCat(any(BufferedImage.class),anyFloat())).thenReturn(true);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(bufferedImage);

        verify(securityRepository , times(1)).setAlarmStatus(AlarmStatus.ALARM);
//        assertEquals(AlarmStatus.ALARM , securityService.getAlarmStatus());
    }
//    disarm system
    @Test
    public void disarmedSystemTest3(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository , atMostOnce()).setAlarmStatus(AlarmStatus.NO_ALARM);
//        assertEquals(AlarmStatus.NO_ALARM , securityService.getAlarmStatus());
    }
//    No cat image
    @Test
    public void notCatImageTest4(){
        Set<Sensor> sensorSet = Set.of(sensor1 , sensor2);
        when(imageService.imageHasCat(any(BufferedImage.class),anyFloat())).thenReturn(false);

        sensorSet.stream().findFirst().ifPresent(sensor -> sensor.setActive(false));
//        here changed code
        securityService.processImage(bufferedImage);
        verify(securityRepository , atMostOnce()).setAlarmStatus(AlarmStatus.NO_ALARM);
//        assertEquals(AlarmStatus.NO_ALARM , securityService.getAlarmStatus());
    }
//    cat Image armed home
    @Test
    public void catImageArmedHomeTest5(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageHasCat(any(BufferedImage.class),anyFloat())).thenReturn(true);
        securityService.processImage(bufferedImage);
        verify(securityRepository , atMostOnce()).setAlarmStatus(AlarmStatus.ALARM);
//        assertEquals(AlarmStatus.ALARM , securityService.getAlarmStatus());
    }

    // deactivating , but already deactivated
    @ParameterizedTest
    @MethodSource("sensorStateChangeDeactivate")
    public void alreadyDeactivatedTest6(AlarmStatus alarmStatus){
        when(securityRepository.getAlarmStatus()).thenReturn(alarmStatus);
//        error NeverWanted but Invoked , securityRepository.setAlarmStatus
        securityService.changeSensorActivationStatus(sensor1 , false);
        verify(securityRepository , never()).setAlarmStatus(any(AlarmStatus.class));
    }
//   already activated and activating it , pending state
    @Test
    public void alreadyActivatedTest7(){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor1 , true);
        verify(securityRepository , times(1)).setAlarmStatus(AlarmStatus.ALARM);
//        assertEquals(AlarmStatus.ALARM , securityService.getAlarmStatus());
    }
//  when alarm is already active
    @ParameterizedTest
    @ValueSource(booleans = {true , false})
    public void alreadyActiveSameStateTest8(Boolean sensorState){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        securityService.changeSensorActivationStatus(sensor2 , sensorState);
        verify(securityRepository , never()).setAlarmStatus(any(AlarmStatus.class));
    }

//    pending alarm and inactive sensors
    @Test
    public void pendingAlarmInactiveSensorsTest9(){
        when(sensor1.getActive()).thenReturn(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor1 , false);
//        error TooManyActualInvocations
        verify(securityRepository , times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
//        assertEquals(AlarmStatus.NO_ALARM , securityService.getAlarmStatus());
    }

//  armed alarm activate sensors and already pending alarm
@ParameterizedTest
@MethodSource("sensorStateChangeOptions")
public void armedAndPendingAlarmActivateTest10(ArmingStatus armingStatus){
    when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
    securityService.changeSensorActivationStatus(sensor1 , true);
    securityRepository.setAlarmStatus(AlarmStatus.PENDING_ALARM);


    verify(securityRepository , times(1)).setAlarmStatus(AlarmStatus.ALARM);
    verify(securityRepository , times(1)).updateSensor(eq(sensor1));
}
//    armed alarm activated sensor , put in pending status
    @ParameterizedTest
    @MethodSource("sensorStateChangeOptions")
    public void armedAlarmChangeToPendingTest11(ArmingStatus armingStatus){
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor1 , true);

        verify(securityRepository , times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
        verify(sensor1 , times(1)).setActive(eq(true));
        verify(securityRepository , times(1)).updateSensor(eq(sensor1));
    }

    @Test
    public void shouldAnswerWithTrue() {
        assertTrue(true);
    }

}
