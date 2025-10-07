package io.armadaproject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import api.SubmitOuterClass.JobSubmitRequest;
import api.SubmitOuterClass.JobSubmitRequestItem;
import io.armadaproject.jenkins.plugin.job.ArmadaMapper;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerResizePolicy;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.VolumeDevice;
import io.fabric8.kubernetes.api.model.VolumeMount;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import k8s.io.api.core.v1.Generated;
import k8s.io.api.core.v1.Generated.Container;
import k8s.io.api.core.v1.Generated.EnvFromSource;
import k8s.io.api.core.v1.Generated.EnvVar;
import k8s.io.api.core.v1.Generated.Lifecycle;
import k8s.io.api.core.v1.Generated.PodResourceClaim;
import k8s.io.api.core.v1.Generated.PodSchedulingGate;
import k8s.io.api.core.v1.Generated.Probe;
import k8s.io.api.core.v1.Generated.ProbeHandler;
import k8s.io.api.core.v1.Generated.ResourceRequirements;
import k8s.io.api.core.v1.Generated.SecurityContext;
import k8s.io.api.core.v1.Generated.Sysctl;
import k8s.io.api.core.v1.Generated.TopologySpreadConstraint;
import k8s.io.apimachinery.pkg.api.resource.Generated.Quantity;
import k8s.io.apimachinery.pkg.apis.meta.v1.Generated.LabelSelector;
import k8s.io.apimachinery.pkg.apis.meta.v1.Generated.LabelSelectorRequirement;
import k8s.io.apimachinery.pkg.util.intstr.Generated.IntOrString;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ArmadaMapperTest {

  private static ArmadaMapper armadaMapper;

  @BeforeAll
  static void setUp() {
    armadaMapper = new ArmadaMapper("example", "example", "example", null);
  }

  @ParameterizedTest
  @MethodSource("provideEmptyDirVolumeSourceInputs")
  void testMapEmptyDirVolumeSource(io.fabric8.kubernetes.api.model.EmptyDirVolumeSource input,
      Generated.EmptyDirVolumeSource expected) {
    Generated.EmptyDirVolumeSource actual = armadaMapper.mapEmptyDirVolumeSource(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideEmptyDirVolumeSourceInputs() {
    io.fabric8.kubernetes.api.model.EmptyDirVolumeSource emptyInput =
        new io.fabric8.kubernetes.api.model.EmptyDirVolumeSource();

    Generated.EmptyDirVolumeSource expectedEmptyOutput =
        Generated.EmptyDirVolumeSource.newBuilder().build();

    io.fabric8.kubernetes.api.model.EmptyDirVolumeSource mediumInput =
        new io.fabric8.kubernetes.api.model.EmptyDirVolumeSource();
    mediumInput.setMedium("Memory");

    Generated.EmptyDirVolumeSource expectedMediumOutput = Generated.EmptyDirVolumeSource
        .newBuilder()
        .setMedium("Memory")
        .build();

    io.fabric8.kubernetes.api.model.EmptyDirVolumeSource sizeLimitInput =
        new io.fabric8.kubernetes.api.model.EmptyDirVolumeSource();
    sizeLimitInput.setSizeLimit(new io.fabric8.kubernetes.api.model.Quantity("1Gi"));

    Generated.EmptyDirVolumeSource expectedSizeLimitOutput = Generated.EmptyDirVolumeSource
        .newBuilder()
        .setSizeLimit(Quantity.newBuilder().setString("1Gi").build())
        .build();

    io.fabric8.kubernetes.api.model.EmptyDirVolumeSource fullInput =
        new io.fabric8.kubernetes.api.model.EmptyDirVolumeSource();
    fullInput.setMedium("Memory");
    fullInput.setSizeLimit(new io.fabric8.kubernetes.api.model.Quantity("1Gi"));

    Generated.EmptyDirVolumeSource expectedFullOutput = Generated.EmptyDirVolumeSource
        .newBuilder()
        .setMedium("Memory")
        .setSizeLimit(Quantity.newBuilder().setString("1Gi").build())
        .build();

    return Stream.of(
        Arguments.of(emptyInput, expectedEmptyOutput),
        Arguments.of(mediumInput, expectedMediumOutput),
        Arguments.of(sizeLimitInput, expectedSizeLimitOutput),
        Arguments.of(fullInput, expectedFullOutput)
    );

  }

  @ParameterizedTest
  @MethodSource("provideKeyToPathInputs")
  void testMapKeyToPaths(List<io.fabric8.kubernetes.api.model.KeyToPath> input,
      List<Generated.KeyToPath> expected) {
    Iterable<? extends Generated.KeyToPath> result = armadaMapper.mapKeyToPaths(input);

    assertEquals(expected, result);
  }

  static Stream<Arguments> provideKeyToPathInputs() {
    io.fabric8.kubernetes.api.model.KeyToPath keyToPathWithKey = new io.fabric8.kubernetes.api.model.KeyToPath();
    keyToPathWithKey.setKey("config-key");

    Generated.KeyToPath expectedWithKey = Generated.KeyToPath.newBuilder()
        .setKey("config-key")
        .build();

    io.fabric8.kubernetes.api.model.KeyToPath keyToPathWithPath = new io.fabric8.kubernetes.api.model.KeyToPath();
    keyToPathWithPath.setPath("/path/to/config");

    Generated.KeyToPath expectedWithPath = Generated.KeyToPath.newBuilder()
        .setPath("/path/to/config")
        .build();

    io.fabric8.kubernetes.api.model.KeyToPath keyToPathWithMode = new io.fabric8.kubernetes.api.model.KeyToPath();
    keyToPathWithMode.setMode(420);

    Generated.KeyToPath expectedWithMode = Generated.KeyToPath.newBuilder()
        .setMode(420)
        .build();

    io.fabric8.kubernetes.api.model.KeyToPath keyToPathWithAllFields = new io.fabric8.kubernetes.api.model.KeyToPath();
    keyToPathWithAllFields.setKey("config-key");
    keyToPathWithAllFields.setPath("/path/to/config");
    keyToPathWithAllFields.setMode(420);

    Generated.KeyToPath expectedWithAllFields = Generated.KeyToPath.newBuilder()
        .setKey("config-key")
        .setPath("/path/to/config")
        .setMode(420)
        .build();

    return Stream.of(
        Arguments.of(Collections.singletonList(keyToPathWithKey),
            Collections.singletonList(expectedWithKey)),
        Arguments.of(Collections.singletonList(keyToPathWithPath),
            Collections.singletonList(expectedWithPath)),
        Arguments.of(Collections.singletonList(keyToPathWithMode),
            Collections.singletonList(expectedWithMode)),
        Arguments.of(Collections.singletonList(keyToPathWithAllFields),
            Collections.singletonList(expectedWithAllFields)),
        Arguments.of(Collections.emptyList(), Collections.emptyList())
    );
  }

  @ParameterizedTest
  @MethodSource("provideSecretVolumeSourceInputs")
  void testMapSecretVolumeSource(
      io.fabric8.kubernetes.api.model.SecretVolumeSource input,
      Generated.SecretVolumeSource expected) {

    Generated.SecretVolumeSource result = armadaMapper.mapSecretVolumeSource(input);

    assertEquals(expected, result);
  }

  static Stream<Arguments> provideSecretVolumeSourceInputs() {
    io.fabric8.kubernetes.api.model.SecretVolumeSource secretWithName = new io.fabric8.kubernetes.api.model.SecretVolumeSource();
    secretWithName.setSecretName("my-secret");

    Generated.SecretVolumeSource expectedWithName = Generated.SecretVolumeSource.newBuilder()
        .setSecretName("my-secret")
        .build();

    io.fabric8.kubernetes.api.model.SecretVolumeSource secretWithItems = new io.fabric8.kubernetes.api.model.SecretVolumeSource();
    io.fabric8.kubernetes.api.model.KeyToPath keyToPath = new io.fabric8.kubernetes.api.model.KeyToPath();
    keyToPath.setKey("config-key");
    keyToPath.setPath("/path/to/config");
    secretWithItems.setItems(Collections.singletonList(keyToPath));

    Generated.KeyToPath mappedKeyToPath = Generated.KeyToPath.newBuilder()
        .setKey("config-key")
        .setPath("/path/to/config")
        .build();

    Generated.SecretVolumeSource expectedWithItems = Generated.SecretVolumeSource.newBuilder()
        .addAllItems(Collections.singletonList(mappedKeyToPath))
        .build();

    io.fabric8.kubernetes.api.model.SecretVolumeSource secretWithDefaultMode = new io.fabric8.kubernetes.api.model.SecretVolumeSource();
    secretWithDefaultMode.setDefaultMode(420);

    Generated.SecretVolumeSource expectedWithDefaultMode = Generated.SecretVolumeSource.newBuilder()
        .setDefaultMode(420)
        .build();

    io.fabric8.kubernetes.api.model.SecretVolumeSource secretWithOptional = new io.fabric8.kubernetes.api.model.SecretVolumeSource();
    secretWithOptional.setOptional(true);

    Generated.SecretVolumeSource expectedWithOptional = Generated.SecretVolumeSource.newBuilder()
        .setOptional(true)
        .build();

    io.fabric8.kubernetes.api.model.SecretVolumeSource secretWithAllFields = new io.fabric8.kubernetes.api.model.SecretVolumeSource();
    secretWithAllFields.setSecretName("my-secret");
    secretWithAllFields.setItems(Collections.singletonList(keyToPath));
    secretWithAllFields.setDefaultMode(420);
    secretWithAllFields.setOptional(true);

    Generated.SecretVolumeSource expectedWithAllFields = Generated.SecretVolumeSource.newBuilder()
        .setSecretName("my-secret")
        .addAllItems(Collections.singletonList(mappedKeyToPath))
        .setDefaultMode(420)
        .setOptional(true)
        .build();

    io.fabric8.kubernetes.api.model.SecretVolumeSource emptySecretVolumeSource = new io.fabric8.kubernetes.api.model.SecretVolumeSource();
    Generated.SecretVolumeSource expectedEmpty = Generated.SecretVolumeSource.newBuilder().build();

    return Stream.of(
        Arguments.of(secretWithName, expectedWithName),
        Arguments.of(secretWithItems, expectedWithItems),
        Arguments.of(secretWithDefaultMode, expectedWithDefaultMode),
        Arguments.of(secretWithOptional, expectedWithOptional),
        Arguments.of(secretWithAllFields, expectedWithAllFields),
        Arguments.of(emptySecretVolumeSource, expectedEmpty)
    );
  }

  @ParameterizedTest
  @MethodSource("provideLocalObjectReferenceInputs")
  void testMapLocalObjectReference(
      io.fabric8.kubernetes.api.model.LocalObjectReference input,
      Generated.LocalObjectReference expected) {
    Generated.LocalObjectReference actual = armadaMapper.mapLocalObjectReference(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideLocalObjectReferenceInputs() {
    io.fabric8.kubernetes.api.model.LocalObjectReference emptyInput = new io.fabric8.kubernetes.api.model.LocalObjectReference();
    Generated.LocalObjectReference expectedEmptyOutput = Generated.LocalObjectReference
        .newBuilder().build();

    io.fabric8.kubernetes.api.model.LocalObjectReference nameInput = new io.fabric8.kubernetes.api.model.LocalObjectReference();
    nameInput.setName("my-secret");

    Generated.LocalObjectReference expectedNameOutput = Generated.LocalObjectReference.newBuilder()
        .setName("my-secret")
        .build();

    return Stream.of(
        Arguments.of(emptyInput, expectedEmptyOutput),
        Arguments.of(nameInput, expectedNameOutput)
    );
  }

  @ParameterizedTest
  @MethodSource("provideVolumeSourceInputs")
  void testMapVolumeSource(
      io.fabric8.kubernetes.api.model.Volume input, Generated.VolumeSource expected) {
    Generated.VolumeSource actual = armadaMapper.mapVolumeSource(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideVolumeSourceInputs() {
    io.fabric8.kubernetes.api.model.Volume emptyInput = new io.fabric8.kubernetes.api.model.Volume();
    Generated.VolumeSource expectedEmptyOutput = Generated.VolumeSource.newBuilder().build();

    io.fabric8.kubernetes.api.model.Volume emptyDirInput = new io.fabric8.kubernetes.api.model.Volume();
    io.fabric8.kubernetes.api.model.EmptyDirVolumeSource emptyDir = new io.fabric8.kubernetes.api.model.EmptyDirVolumeSource();
    emptyDir.setMedium("Memory");
    emptyDirInput.setEmptyDir(emptyDir);

    Generated.VolumeSource expectedEmptyDirOutput = Generated.VolumeSource.newBuilder()
        .setEmptyDir(Generated.EmptyDirVolumeSource.newBuilder().setMedium("Memory").build())
        .build();

    io.fabric8.kubernetes.api.model.Volume secretInput = new io.fabric8.kubernetes.api.model.Volume();
    io.fabric8.kubernetes.api.model.SecretVolumeSource secret = new io.fabric8.kubernetes.api.model.SecretVolumeSource();
    secret.setSecretName("my-secret");
    secretInput.setSecret(secret);

    Generated.VolumeSource expectedSecretOutput = Generated.VolumeSource.newBuilder()
        .setSecret(Generated.SecretVolumeSource.newBuilder().setSecretName("my-secret").build())
        .build();

    io.fabric8.kubernetes.api.model.Volume fullInput = new io.fabric8.kubernetes.api.model.Volume();
    fullInput.setEmptyDir(emptyDir);
    fullInput.setSecret(secret);

    Generated.VolumeSource expectedFullOutput = Generated.VolumeSource.newBuilder()
        .setEmptyDir(Generated.EmptyDirVolumeSource.newBuilder().setMedium("Memory").build())
        .setSecret(Generated.SecretVolumeSource.newBuilder().setSecretName("my-secret").build())
        .build();

    return Stream.of(
        Arguments.of(emptyInput, expectedEmptyOutput),
        Arguments.of(emptyDirInput, expectedEmptyDirOutput),
        Arguments.of(secretInput, expectedSecretOutput),
        Arguments.of(fullInput, expectedFullOutput)
    );
  }

  @ParameterizedTest
  @MethodSource("provideVolumesInputs")
  void testMapVolumes(
      List<io.fabric8.kubernetes.api.model.Volume> input, List<Generated.Volume> expected) {
    Iterable<? extends Generated.Volume> actual = armadaMapper.mapVolumes(input);

    assertIterableEquals(expected, actual);
  }

  static Stream<Arguments> provideVolumesInputs() {
    List<io.fabric8.kubernetes.api.model.Volume> emptyInput = List.of();
    List<Generated.Volume> expectedEmptyOutput = List.of();

    io.fabric8.kubernetes.api.model.Volume nameOnlyInput = new io.fabric8.kubernetes.api.model.Volume();
    nameOnlyInput.setName("my-volume");

    Generated.Volume expectedNameOnlyOutput = Generated.Volume.newBuilder()
        .setName("my-volume")
        .setVolumeSource(Generated.VolumeSource.newBuilder().build())
        .build();

    io.fabric8.kubernetes.api.model.Volume volumeSourceOnlyInput = new io.fabric8.kubernetes.api.model.Volume();
    io.fabric8.kubernetes.api.model.EmptyDirVolumeSource emptyDir = new io.fabric8.kubernetes.api.model.EmptyDirVolumeSource();
    emptyDir.setMedium("Memory");
    volumeSourceOnlyInput.setEmptyDir(emptyDir);

    Generated.Volume expectedVolumeSourceOnlyOutput = Generated.Volume.newBuilder()
        .setVolumeSource(Generated.VolumeSource.newBuilder()
            .setEmptyDir(Generated.EmptyDirVolumeSource.newBuilder().setMedium("Memory").build())
            .build())
        .build();

    io.fabric8.kubernetes.api.model.Volume fullInput = new io.fabric8.kubernetes.api.model.Volume();
    fullInput.setName("my-volume");
    fullInput.setEmptyDir(emptyDir);

    Generated.Volume expectedFullOutput = Generated.Volume.newBuilder()
        .setName("my-volume")
        .setVolumeSource(Generated.VolumeSource.newBuilder()
            .setEmptyDir(Generated.EmptyDirVolumeSource.newBuilder().setMedium("Memory").build())
            .build())
        .build();

    return Stream.of(
        Arguments.of(emptyInput, expectedEmptyOutput),
        Arguments.of(List.of(nameOnlyInput), List.of(expectedNameOnlyOutput)),
        Arguments.of(List.of(volumeSourceOnlyInput), List.of(expectedVolumeSourceOnlyOutput)),
        Arguments.of(List.of(fullInput), List.of(expectedFullOutput))
    );
  }

  @ParameterizedTest
  @MethodSource("provideContainerPortsInputs")
  void testMapContainerPorts(
      List<io.fabric8.kubernetes.api.model.ContainerPort> input,
      List<Generated.ContainerPort> expected) {
    Iterable<? extends Generated.ContainerPort> actual = armadaMapper.mapContainerPorts(input);

    assertIterableEquals(expected, actual);
  }

  static Stream<Arguments> provideContainerPortsInputs() {
    List<io.fabric8.kubernetes.api.model.ContainerPort> emptyInput = List.of();
    List<Generated.ContainerPort> expectedEmptyOutput = List.of();

    io.fabric8.kubernetes.api.model.ContainerPort nameOnlyInput = new io.fabric8.kubernetes.api.model.ContainerPort();
    nameOnlyInput.setName("http");

    Generated.ContainerPort expectedNameOnlyOutput = Generated.ContainerPort.newBuilder()
        .setName("http")
        .build();

    io.fabric8.kubernetes.api.model.ContainerPort fullInput = new io.fabric8.kubernetes.api.model.ContainerPort();
    fullInput.setName("http");
    fullInput.setHostPort(8080);
    fullInput.setContainerPort(80);
    fullInput.setProtocol("TCP");
    fullInput.setHostIP("127.0.0.1");

    Generated.ContainerPort expectedFullOutput = Generated.ContainerPort.newBuilder()
        .setName("http")
        .setHostPort(8080)
        .setContainerPort(80)
        .setProtocol("TCP")
        .setHostIP("127.0.0.1")
        .build();

    return Stream.of(
        Arguments.of(emptyInput, expectedEmptyOutput),
        Arguments.of(List.of(nameOnlyInput), List.of(expectedNameOnlyOutput)),
        Arguments.of(List.of(fullInput), List.of(expectedFullOutput))
    );
  }

  @ParameterizedTest
  @MethodSource("provideObjectFieldSelectorInputs")
  void testMapObjectFieldSelector(
      io.fabric8.kubernetes.api.model.ObjectFieldSelector input,
      Generated.ObjectFieldSelector expected) {
    Generated.ObjectFieldSelector actual = armadaMapper.mapObjectFieldSelector(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideObjectFieldSelectorInputs() {
    io.fabric8.kubernetes.api.model.ObjectFieldSelector emptyInput = new io.fabric8.kubernetes.api.model.ObjectFieldSelector();
    Generated.ObjectFieldSelector expectedEmptyOutput = Generated.ObjectFieldSelector.newBuilder()
        .build();

    io.fabric8.kubernetes.api.model.ObjectFieldSelector apiVersionOnlyInput = new io.fabric8.kubernetes.api.model.ObjectFieldSelector();
    apiVersionOnlyInput.setApiVersion("v1");

    Generated.ObjectFieldSelector expectedApiVersionOnlyOutput = Generated.ObjectFieldSelector.newBuilder()
        .setApiVersion("v1")
        .build();

    io.fabric8.kubernetes.api.model.ObjectFieldSelector fieldPathOnlyInput = new io.fabric8.kubernetes.api.model.ObjectFieldSelector();
    fieldPathOnlyInput.setFieldPath("metadata.name");

    Generated.ObjectFieldSelector expectedFieldPathOnlyOutput = Generated.ObjectFieldSelector.newBuilder()
        .setFieldPath("metadata.name")
        .build();

    io.fabric8.kubernetes.api.model.ObjectFieldSelector fullInput = new io.fabric8.kubernetes.api.model.ObjectFieldSelector();
    fullInput.setApiVersion("v1");
    fullInput.setFieldPath("metadata.name");

    Generated.ObjectFieldSelector expectedFullOutput = Generated.ObjectFieldSelector.newBuilder()
        .setApiVersion("v1")
        .setFieldPath("metadata.name")
        .build();

    return Stream.of(
        Arguments.of(emptyInput, expectedEmptyOutput),
        Arguments.of(apiVersionOnlyInput, expectedApiVersionOnlyOutput),
        Arguments.of(fieldPathOnlyInput, expectedFieldPathOnlyOutput),
        Arguments.of(fullInput, expectedFullOutput)
    );
  }

  @ParameterizedTest
  @MethodSource("provideResourceFieldSelectorInputs")
  void testMapResourceFieldSelector(
      io.fabric8.kubernetes.api.model.ResourceFieldSelector input,
      Generated.ResourceFieldSelector expected) {
    Generated.ResourceFieldSelector actual = armadaMapper.mapResourceFieldSelector(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideResourceFieldSelectorInputs() {
    io.fabric8.kubernetes.api.model.ResourceFieldSelector emptyInput = new io.fabric8.kubernetes.api.model.ResourceFieldSelector();
    Generated.ResourceFieldSelector expectedEmptyOutput = Generated.ResourceFieldSelector.newBuilder()
        .build();

    io.fabric8.kubernetes.api.model.ResourceFieldSelector containerNameOnlyInput = new io.fabric8.kubernetes.api.model.ResourceFieldSelector();
    containerNameOnlyInput.setContainerName("my-container");

    Generated.ResourceFieldSelector expectedContainerNameOnlyOutput = Generated.ResourceFieldSelector.newBuilder()
        .setContainerName("my-container")
        .build();

    io.fabric8.kubernetes.api.model.ResourceFieldSelector resourceOnlyInput = new io.fabric8.kubernetes.api.model.ResourceFieldSelector();
    resourceOnlyInput.setResource("cpu");

    Generated.ResourceFieldSelector expectedResourceOnlyOutput = Generated.ResourceFieldSelector.newBuilder()
        .setResource("cpu")
        .build();

    io.fabric8.kubernetes.api.model.ResourceFieldSelector divisorOnlyInput = new io.fabric8.kubernetes.api.model.ResourceFieldSelector();
    io.fabric8.kubernetes.api.model.Quantity divisor = new io.fabric8.kubernetes.api.model.Quantity();
    divisor.setAmount("1Gi");
    divisorOnlyInput.setDivisor(divisor);

    Generated.ResourceFieldSelector expectedDivisorOnlyOutput = Generated.ResourceFieldSelector.newBuilder()
        .setDivisor(Quantity.newBuilder().setString("1Gi").build())
        .build();

    io.fabric8.kubernetes.api.model.ResourceFieldSelector fullInput = new io.fabric8.kubernetes.api.model.ResourceFieldSelector();
    fullInput.setContainerName("my-container");
    fullInput.setResource("cpu");
    fullInput.setDivisor(divisor);

    Generated.ResourceFieldSelector expectedFullOutput = Generated.ResourceFieldSelector.newBuilder()
        .setContainerName("my-container")
        .setResource("cpu")
        .setDivisor(Quantity.newBuilder().setString("1Gi").build())
        .build();

    return Stream.of(
        Arguments.of(emptyInput, expectedEmptyOutput),
        Arguments.of(containerNameOnlyInput, expectedContainerNameOnlyOutput),
        Arguments.of(resourceOnlyInput, expectedResourceOnlyOutput),
        Arguments.of(divisorOnlyInput, expectedDivisorOnlyOutput),
        Arguments.of(fullInput, expectedFullOutput)
    );
  }

  @ParameterizedTest
  @MethodSource("provideLocalObjectReferenceStringInputs")
  void testMapLocalObjectReference(String input, Generated.LocalObjectReference expected) {
    Generated.LocalObjectReference actual = armadaMapper.mapLocalObjectReference(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideLocalObjectReferenceStringInputs() {
    return Stream.of(
        Arguments.of(null, Generated.LocalObjectReference.newBuilder().build()),
        Arguments.of("my-secret",
            Generated.LocalObjectReference.newBuilder().setName("my-secret").build())
    );
  }

  @ParameterizedTest
  @MethodSource("provideConfigMapKeySelectorInputs")
  void testMapConfigMapKeySelector(io.fabric8.kubernetes.api.model.ConfigMapKeySelector input,
      Generated.ConfigMapKeySelector expected) {
    Generated.ConfigMapKeySelector actual = armadaMapper.mapConfigMapKeySelector(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideConfigMapKeySelectorInputs() {
    io.fabric8.kubernetes.api.model.ConfigMapKeySelector inputWithNoFields = new io.fabric8.kubernetes.api.model.ConfigMapKeySelector();
    Generated.ConfigMapKeySelector expectedWithNoFields = Generated.ConfigMapKeySelector.newBuilder()
        .build();

    io.fabric8.kubernetes.api.model.ConfigMapKeySelector inputWithNameOnly = new io.fabric8.kubernetes.api.model.ConfigMapKeySelector();
    inputWithNameOnly.setName("my-config");
    Generated.ConfigMapKeySelector expectedWithNameOnly = Generated.ConfigMapKeySelector.newBuilder()
        .setLocalObjectReference(
            Generated.LocalObjectReference.newBuilder().setName("my-config").build())
        .build();

    io.fabric8.kubernetes.api.model.ConfigMapKeySelector inputWithKeyOnly = new io.fabric8.kubernetes.api.model.ConfigMapKeySelector();
    inputWithKeyOnly.setKey("my-key");
    Generated.ConfigMapKeySelector expectedWithKeyOnly = Generated.ConfigMapKeySelector.newBuilder()
        .setKey("my-key")
        .build();

    io.fabric8.kubernetes.api.model.ConfigMapKeySelector inputWithOptionalOnly = new io.fabric8.kubernetes.api.model.ConfigMapKeySelector();
    inputWithOptionalOnly.setOptional(true);
    Generated.ConfigMapKeySelector expectedWithOptionalOnly = Generated.ConfigMapKeySelector.newBuilder()
        .setOptional(true)
        .build();

    io.fabric8.kubernetes.api.model.ConfigMapKeySelector inputWithAllFields = new io.fabric8.kubernetes.api.model.ConfigMapKeySelector();
    inputWithAllFields.setName("my-config");
    inputWithAllFields.setKey("my-key");
    inputWithAllFields.setOptional(true);

    Generated.ConfigMapKeySelector expectedWithAllFields = Generated.ConfigMapKeySelector.newBuilder()
        .setLocalObjectReference(
            Generated.LocalObjectReference.newBuilder().setName("my-config").build())
        .setKey("my-key")
        .setOptional(true)
        .build();

    return Stream.of(
        Arguments.of(inputWithNoFields, expectedWithNoFields),
        Arguments.of(inputWithNameOnly, expectedWithNameOnly),
        Arguments.of(inputWithKeyOnly, expectedWithKeyOnly),
        Arguments.of(inputWithOptionalOnly, expectedWithOptionalOnly),
        Arguments.of(inputWithAllFields, expectedWithAllFields)
    );
  }

  @ParameterizedTest
  @MethodSource("provideSecretKeySelectorInputs")
  void testMapSecretKeySelector(io.fabric8.kubernetes.api.model.SecretKeySelector input,
      Generated.SecretKeySelector expected) {
    Generated.SecretKeySelector actual = armadaMapper.mapSecretKeySelector(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideSecretKeySelectorInputs() {
    io.fabric8.kubernetes.api.model.SecretKeySelector inputWithNoFields = new io.fabric8.kubernetes.api.model.SecretKeySelector();
    Generated.SecretKeySelector expectedWithNoFields = Generated.SecretKeySelector.newBuilder()
        .build();

    io.fabric8.kubernetes.api.model.SecretKeySelector inputWithNameOnly = new io.fabric8.kubernetes.api.model.SecretKeySelector();
    inputWithNameOnly.setName("my-secret");
    Generated.SecretKeySelector expectedWithNameOnly = Generated.SecretKeySelector.newBuilder()
        .setLocalObjectReference(
            Generated.LocalObjectReference.newBuilder().setName("my-secret").build())
        .build();

    io.fabric8.kubernetes.api.model.SecretKeySelector inputWithKeyOnly = new io.fabric8.kubernetes.api.model.SecretKeySelector();
    inputWithKeyOnly.setKey("my-key");
    Generated.SecretKeySelector expectedWithKeyOnly = Generated.SecretKeySelector.newBuilder()
        .setKey("my-key")
        .build();

    io.fabric8.kubernetes.api.model.SecretKeySelector inputWithOptionalOnly = new io.fabric8.kubernetes.api.model.SecretKeySelector();
    inputWithOptionalOnly.setOptional(true);
    Generated.SecretKeySelector expectedWithOptionalOnly = Generated.SecretKeySelector.newBuilder()
        .setOptional(true)
        .build();

    io.fabric8.kubernetes.api.model.SecretKeySelector inputWithAllFields = new io.fabric8.kubernetes.api.model.SecretKeySelector();
    inputWithAllFields.setName("my-secret");
    inputWithAllFields.setKey("my-key");
    inputWithAllFields.setOptional(true);

    Generated.SecretKeySelector expectedWithAllFields = Generated.SecretKeySelector.newBuilder()
        .setLocalObjectReference(
            Generated.LocalObjectReference.newBuilder().setName("my-secret").build())
        .setKey("my-key")
        .setOptional(true)
        .build();

    return Stream.of(
        Arguments.of(inputWithNoFields, expectedWithNoFields),
        Arguments.of(inputWithNameOnly, expectedWithNameOnly),
        Arguments.of(inputWithKeyOnly, expectedWithKeyOnly),
        Arguments.of(inputWithOptionalOnly, expectedWithOptionalOnly),
        Arguments.of(inputWithAllFields, expectedWithAllFields)
    );
  }

  @ParameterizedTest
  @MethodSource("provideEnvVarSourceInputs")
  void testMapEnvVarSource(io.fabric8.kubernetes.api.model.EnvVarSource input,
      Generated.EnvVarSource expected) {
    Generated.EnvVarSource actual = armadaMapper.mapEnvVarSource(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideEnvVarSourceInputs() {
    io.fabric8.kubernetes.api.model.EnvVarSource inputWithNoFields = new io.fabric8.kubernetes.api.model.EnvVarSource();
    Generated.EnvVarSource expectedWithNoFields = Generated.EnvVarSource.newBuilder().build();

    io.fabric8.kubernetes.api.model.EnvVarSource inputWithFieldRefOnly = new io.fabric8.kubernetes.api.model.EnvVarSource();
    inputWithFieldRefOnly.setFieldRef(new io.fabric8.kubernetes.api.model.ObjectFieldSelector());
    Generated.EnvVarSource expectedWithFieldRefOnly = Generated.EnvVarSource.newBuilder()
        .setFieldRef(Generated.ObjectFieldSelector.newBuilder().build())
        .build();

    io.fabric8.kubernetes.api.model.EnvVarSource inputWithResourceFieldRefOnly = new io.fabric8.kubernetes.api.model.EnvVarSource();
    inputWithResourceFieldRefOnly.setResourceFieldRef(
        new io.fabric8.kubernetes.api.model.ResourceFieldSelector());
    Generated.EnvVarSource expectedWithResourceFieldRefOnly = Generated.EnvVarSource.newBuilder()
        .setResourceFieldRef(Generated.ResourceFieldSelector.newBuilder().build())
        .build();

    io.fabric8.kubernetes.api.model.EnvVarSource inputWithConfigMapKeyRefOnly = new io.fabric8.kubernetes.api.model.EnvVarSource();
    inputWithConfigMapKeyRefOnly.setConfigMapKeyRef(
        new io.fabric8.kubernetes.api.model.ConfigMapKeySelector());
    Generated.EnvVarSource expectedWithConfigMapKeyRefOnly = Generated.EnvVarSource.newBuilder()
        .setConfigMapKeyRef(Generated.ConfigMapKeySelector.newBuilder().build())
        .build();

    io.fabric8.kubernetes.api.model.EnvVarSource inputWithSecretKeyRefOnly = new io.fabric8.kubernetes.api.model.EnvVarSource();
    inputWithSecretKeyRefOnly.setSecretKeyRef(
        new io.fabric8.kubernetes.api.model.SecretKeySelector());
    Generated.EnvVarSource expectedWithSecretKeyRefOnly = Generated.EnvVarSource.newBuilder()
        .setSecretKeyRef(Generated.SecretKeySelector.newBuilder().build())
        .build();

    io.fabric8.kubernetes.api.model.EnvVarSource inputWithAllFields =
        new io.fabric8.kubernetes.api.model.EnvVarSource();
    inputWithAllFields.setFieldRef(new io.fabric8.kubernetes.api.model.ObjectFieldSelector());
    inputWithAllFields.setResourceFieldRef(
        new io.fabric8.kubernetes.api.model.ResourceFieldSelector());
    inputWithAllFields.setConfigMapKeyRef(
        new io.fabric8.kubernetes.api.model.ConfigMapKeySelector());
    inputWithAllFields.setSecretKeyRef(new io.fabric8.kubernetes.api.model.SecretKeySelector());

    Generated.EnvVarSource expectedWithAllFields = Generated.EnvVarSource.newBuilder()
        .setFieldRef(Generated.ObjectFieldSelector.newBuilder().build())
        .setResourceFieldRef(Generated.ResourceFieldSelector.newBuilder().build())
        .setConfigMapKeyRef(Generated.ConfigMapKeySelector.newBuilder().build())
        .setSecretKeyRef(Generated.SecretKeySelector.newBuilder().build())
        .build();

    return Stream.of(
        Arguments.of(inputWithNoFields, expectedWithNoFields),
        Arguments.of(inputWithFieldRefOnly, expectedWithFieldRefOnly),
        Arguments.of(inputWithResourceFieldRefOnly, expectedWithResourceFieldRefOnly),
        Arguments.of(inputWithConfigMapKeyRefOnly, expectedWithConfigMapKeyRefOnly),
        Arguments.of(inputWithSecretKeyRefOnly, expectedWithSecretKeyRefOnly),
        Arguments.of(inputWithAllFields, expectedWithAllFields)
    );
  }

  @ParameterizedTest
  @MethodSource("provideEnvVarInputs")
  void testMapEnvVars(List<io.fabric8.kubernetes.api.model.EnvVar> input,
      Iterable<Generated.EnvVar> expected) {
    Iterable<? extends EnvVar> actual = armadaMapper.mapEnvVars(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideEnvVarInputs() {
    io.fabric8.kubernetes.api.model.EnvVar inputWithNoFields = new io.fabric8.kubernetes.api.model.EnvVar();
    Generated.EnvVar expectedWithNoFields = Generated.EnvVar.newBuilder().build();

    io.fabric8.kubernetes.api.model.EnvVar inputWithNameOnly = new io.fabric8.kubernetes.api.model.EnvVar();
    inputWithNameOnly.setName("MY_ENV_VAR");
    Generated.EnvVar expectedWithNameOnly = Generated.EnvVar.newBuilder()
        .setName("MY_ENV_VAR")
        .build();

    io.fabric8.kubernetes.api.model.EnvVar inputWithValueOnly = new io.fabric8.kubernetes.api.model.EnvVar();
    inputWithValueOnly.setValue("some-value");
    Generated.EnvVar expectedWithValueOnly = Generated.EnvVar.newBuilder()
        .setValue("some-value")
        .build();

    io.fabric8.kubernetes.api.model.EnvVar inputWithValueFromOnly = new io.fabric8.kubernetes.api.model.EnvVar();
    inputWithValueFromOnly.setValueFrom(new io.fabric8.kubernetes.api.model.EnvVarSource());
    Generated.EnvVar expectedWithValueFromOnly = Generated.EnvVar.newBuilder()
        .setValueFrom(Generated.EnvVarSource.newBuilder().build())
        .build();

    io.fabric8.kubernetes.api.model.EnvVar inputWithAllFields = new io.fabric8.kubernetes.api.model.EnvVar();
    inputWithAllFields.setName("MY_ENV_VAR");
    inputWithAllFields.setValue("some-value");
    inputWithAllFields.setValueFrom(new io.fabric8.kubernetes.api.model.EnvVarSource());

    Generated.EnvVar expectedWithAllFields = Generated.EnvVar.newBuilder()
        .setName("MY_ENV_VAR")
        .setValue("some-value")
        .setValueFrom(Generated.EnvVarSource.newBuilder().build())
        .build();

    return Stream.of(
        Arguments.of(List.of(inputWithNoFields), List.of(expectedWithNoFields)),
        Arguments.of(List.of(inputWithNameOnly), List.of(expectedWithNameOnly)),
        Arguments.of(List.of(inputWithValueOnly), List.of(expectedWithValueOnly)),
        Arguments.of(List.of(inputWithValueFromOnly), List.of(expectedWithValueFromOnly)),
        Arguments.of(List.of(inputWithAllFields), List.of(expectedWithAllFields))
    );
  }

  @ParameterizedTest
  @MethodSource("provideConfigMapEnvSourceInputs")
  void testMapConfigMapEnvSource(io.fabric8.kubernetes.api.model.ConfigMapEnvSource input,
      Generated.ConfigMapEnvSource expected) {
    Generated.ConfigMapEnvSource actual = armadaMapper.mapConfigMapEnvSource(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideConfigMapEnvSourceInputs() {
    io.fabric8.kubernetes.api.model.ConfigMapEnvSource inputWithNoFields = new io.fabric8.kubernetes.api.model.ConfigMapEnvSource();
    Generated.ConfigMapEnvSource expectedWithNoFields = Generated.ConfigMapEnvSource.newBuilder()
        .build();

    io.fabric8.kubernetes.api.model.ConfigMapEnvSource inputWithNameOnly = new io.fabric8.kubernetes.api.model.ConfigMapEnvSource();
    inputWithNameOnly.setName("MY_CONFIG_MAP");
    Generated.ConfigMapEnvSource expectedWithNameOnly = Generated.ConfigMapEnvSource.newBuilder()
        .setLocalObjectReference(
            Generated.LocalObjectReference.newBuilder().setName("MY_CONFIG_MAP").build())
        .build();

    io.fabric8.kubernetes.api.model.ConfigMapEnvSource inputWithOptionalOnly = new io.fabric8.kubernetes.api.model.ConfigMapEnvSource();
    inputWithOptionalOnly.setOptional(true);
    Generated.ConfigMapEnvSource expectedWithOptionalOnly = Generated.ConfigMapEnvSource.newBuilder()
        .setOptional(true)
        .build();

    io.fabric8.kubernetes.api.model.ConfigMapEnvSource inputWithAllFields = new io.fabric8.kubernetes.api.model.ConfigMapEnvSource();
    inputWithAllFields.setName("MY_CONFIG_MAP");
    inputWithAllFields.setOptional(true);

    Generated.ConfigMapEnvSource expectedWithAllFields = Generated.ConfigMapEnvSource.newBuilder()
        .setLocalObjectReference(
            Generated.LocalObjectReference.newBuilder().setName("MY_CONFIG_MAP").build())
        .setOptional(true)
        .build();

    return Stream.of(
        Arguments.of(inputWithNoFields, expectedWithNoFields),
        Arguments.of(inputWithNameOnly, expectedWithNameOnly),
        Arguments.of(inputWithOptionalOnly, expectedWithOptionalOnly),
        Arguments.of(inputWithAllFields, expectedWithAllFields)
    );
  }

  @ParameterizedTest
  @MethodSource("provideSecretEnvSourceInputs")
  void testMapSecretEnvSource(io.fabric8.kubernetes.api.model.SecretEnvSource input,
      Generated.SecretEnvSource expected) {
    Generated.SecretEnvSource actual = armadaMapper.mapSecretEnvSource(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideSecretEnvSourceInputs() {
    io.fabric8.kubernetes.api.model.SecretEnvSource inputWithNoFields = new io.fabric8.kubernetes.api.model.SecretEnvSource();
    Generated.SecretEnvSource expectedWithNoFields = Generated.SecretEnvSource.newBuilder().build();

    io.fabric8.kubernetes.api.model.SecretEnvSource inputWithNameOnly = new io.fabric8.kubernetes.api.model.SecretEnvSource();
    inputWithNameOnly.setName("MY_SECRET");
    Generated.SecretEnvSource expectedWithNameOnly = Generated.SecretEnvSource.newBuilder()
        .setLocalObjectReference(
            Generated.LocalObjectReference.newBuilder().setName("MY_SECRET").build())
        .build();

    io.fabric8.kubernetes.api.model.SecretEnvSource inputWithOptionalOnly = new io.fabric8.kubernetes.api.model.SecretEnvSource();
    inputWithOptionalOnly.setOptional(true);
    Generated.SecretEnvSource expectedWithOptionalOnly = Generated.SecretEnvSource.newBuilder()
        .setOptional(true)
        .build();

    io.fabric8.kubernetes.api.model.SecretEnvSource inputWithAllFields = new io.fabric8.kubernetes.api.model.SecretEnvSource();
    inputWithAllFields.setName("MY_SECRET");
    inputWithAllFields.setOptional(true);

    Generated.SecretEnvSource expectedWithAllFields = Generated.SecretEnvSource.newBuilder()
        .setLocalObjectReference(
            Generated.LocalObjectReference.newBuilder().setName("MY_SECRET").build())
        .setOptional(true)
        .build();

    return Stream.of(
        Arguments.of(inputWithNoFields, expectedWithNoFields),
        Arguments.of(inputWithNameOnly, expectedWithNameOnly),
        Arguments.of(inputWithOptionalOnly, expectedWithOptionalOnly),
        Arguments.of(inputWithAllFields, expectedWithAllFields)
    );
  }

  @ParameterizedTest
  @MethodSource("provideEnvFromSourceInputs")
  void testMapEnvFromSource(List<io.fabric8.kubernetes.api.model.EnvFromSource> input,
      List<Generated.EnvFromSource> expected) {
    Iterable<? extends EnvFromSource> actual = armadaMapper.mapEnvFromSource(input);

    assertIterableEquals(expected, actual);
  }

  static Stream<Arguments> provideEnvFromSourceInputs() {
    io.fabric8.kubernetes.api.model.EnvFromSource inputWithNoFields = new io.fabric8.kubernetes.api.model.EnvFromSource();
    List<io.fabric8.kubernetes.api.model.EnvFromSource> inputListWithNoFields = Collections.singletonList(
        inputWithNoFields);
    List<Generated.EnvFromSource> expectedWithNoFields = Collections.singletonList(
        Generated.EnvFromSource.newBuilder().build());

    io.fabric8.kubernetes.api.model.EnvFromSource inputWithConfigMapRefOnly = new io.fabric8.kubernetes.api.model.EnvFromSource();
    io.fabric8.kubernetes.api.model.ConfigMapEnvSource configMapEnvSource = new io.fabric8.kubernetes.api.model.ConfigMapEnvSource();
    configMapEnvSource.setName("MY_CONFIG_MAP");
    inputWithConfigMapRefOnly.setConfigMapRef(configMapEnvSource);
    List<io.fabric8.kubernetes.api.model.EnvFromSource> inputListWithConfigMapRefOnly = Collections.singletonList(
        inputWithConfigMapRefOnly);
    List<Generated.EnvFromSource> expectedWithConfigMapRefOnly = Collections.singletonList(
        Generated.EnvFromSource.newBuilder()
            .setConfigMapRef(Generated.ConfigMapEnvSource.newBuilder().setLocalObjectReference(
                    Generated.LocalObjectReference.newBuilder().setName("MY_CONFIG_MAP").build())
                .build())
            .build());

    io.fabric8.kubernetes.api.model.EnvFromSource inputWithSecretRefOnly = new io.fabric8.kubernetes.api.model.EnvFromSource();
    io.fabric8.kubernetes.api.model.SecretEnvSource secretEnvSource = new io.fabric8.kubernetes.api.model.SecretEnvSource();
    secretEnvSource.setName("MY_SECRET");
    inputWithSecretRefOnly.setSecretRef(secretEnvSource);
    List<io.fabric8.kubernetes.api.model.EnvFromSource> inputListWithSecretRefOnly = Collections.singletonList(
        inputWithSecretRefOnly);
    List<Generated.EnvFromSource> expectedWithSecretRefOnly = Collections.singletonList(
        Generated.EnvFromSource.newBuilder()
            .setSecretRef(Generated.SecretEnvSource.newBuilder().setLocalObjectReference(
                Generated.LocalObjectReference.newBuilder().setName("MY_SECRET").build()).build())
            .build());

    io.fabric8.kubernetes.api.model.EnvFromSource inputWithAllFields = new io.fabric8.kubernetes.api.model.EnvFromSource();
    inputWithAllFields.setConfigMapRef(configMapEnvSource);
    inputWithAllFields.setSecretRef(secretEnvSource);
    List<io.fabric8.kubernetes.api.model.EnvFromSource> inputListWithAllFields = Collections.singletonList(
        inputWithAllFields);
    List<Generated.EnvFromSource> expectedWithAllFields = Collections.singletonList(
        Generated.EnvFromSource.newBuilder()
            .setConfigMapRef(Generated.ConfigMapEnvSource.newBuilder().setLocalObjectReference(
                    Generated.LocalObjectReference.newBuilder().setName("MY_CONFIG_MAP").build())
                .build())
            .setSecretRef(Generated.SecretEnvSource.newBuilder().setLocalObjectReference(
                Generated.LocalObjectReference.newBuilder().setName("MY_SECRET").build()).build())
            .build());

    return Stream.of(
        Arguments.of(inputListWithNoFields, expectedWithNoFields),
        Arguments.of(inputListWithConfigMapRefOnly, expectedWithConfigMapRefOnly),
        Arguments.of(inputListWithSecretRefOnly, expectedWithSecretRefOnly),
        Arguments.of(inputListWithAllFields, expectedWithAllFields)
    );
  }

  @ParameterizedTest
  @MethodSource("provideResourceLimitsInputs")
  void testMapResourceLimits(Map<String, io.fabric8.kubernetes.api.model.Quantity> input,
      Map<String, Quantity> expected) {
    Map<String, Quantity> actual = armadaMapper.mapResourceLimits(input);

    assertIterableEquals(expected.entrySet(), actual.entrySet());
  }

  static Stream<Arguments> provideResourceLimitsInputs() {
    io.fabric8.kubernetes.api.model.Quantity quantity = new io.fabric8.kubernetes.api.model.Quantity();
    quantity.setAmount("10");
    quantity.setFormat("Gi");
    Map<String, io.fabric8.kubernetes.api.model.Quantity> inputWithNoFields = Collections.emptyMap();
    Map<String, Quantity> expectedWithNoFields = Collections.emptyMap();

    Map<String, io.fabric8.kubernetes.api.model.Quantity> inputWithOneLimit = new HashMap<>();
    inputWithOneLimit.put("cpu", quantity);
    Map<String, Quantity> expectedWithOneLimit = new HashMap<>();
    expectedWithOneLimit.put("cpu", Quantity.newBuilder().setString("10Gi").build());

    Map<String, io.fabric8.kubernetes.api.model.Quantity> inputWithMultipleLimits = new HashMap<>();
    inputWithMultipleLimits.put("cpu", quantity);
    inputWithMultipleLimits.put("memory", quantity);
    Map<String, Quantity> expectedWithMultipleLimits = new HashMap<>();
    expectedWithMultipleLimits.put("cpu", Quantity.newBuilder().setString("10Gi").build());
    expectedWithMultipleLimits.put("memory", Quantity.newBuilder().setString("10Gi").build());

    return Stream.of(
        Arguments.of(inputWithNoFields, expectedWithNoFields),
        Arguments.of(inputWithOneLimit, expectedWithOneLimit),
        Arguments.of(inputWithMultipleLimits, expectedWithMultipleLimits)
    );
  }

  @ParameterizedTest
  @MethodSource("provideResourceRequestsInputs")
  void testMapResourceRequests(Map<String, io.fabric8.kubernetes.api.model.Quantity> input,
      Map<String, Quantity> expected) {
    Map<String, Quantity> actual = armadaMapper.mapResourceRequests(input);

    assertIterableEquals(expected.entrySet(), actual.entrySet());
  }

  static Stream<Arguments> provideResourceRequestsInputs() {
    io.fabric8.kubernetes.api.model.Quantity quantity = new io.fabric8.kubernetes.api.model.Quantity();
    quantity.setAmount("10");
    quantity.setFormat("Gi");
    Map<String, io.fabric8.kubernetes.api.model.Quantity> inputWithNoFields = Collections.emptyMap();
    Map<String, Quantity> expectedWithNoFields = Collections.emptyMap();

    Map<String, io.fabric8.kubernetes.api.model.Quantity> inputWithOneRequest = new HashMap<>();
    inputWithOneRequest.put("cpu", quantity);
    Map<String, Quantity> expectedWithOneRequest = new HashMap<>();
    expectedWithOneRequest.put("cpu", Quantity.newBuilder().setString("10Gi").build());

    Map<String, io.fabric8.kubernetes.api.model.Quantity> inputWithMultipleRequests = new HashMap<>();
    inputWithMultipleRequests.put("cpu", quantity);
    inputWithMultipleRequests.put("memory", quantity);
    Map<String, Quantity> expectedWithMultipleRequests = new HashMap<>();
    expectedWithMultipleRequests.put("cpu", Quantity.newBuilder().setString("10Gi").build());
    expectedWithMultipleRequests.put("memory", Quantity.newBuilder().setString("10Gi").build());

    return Stream.of(
        Arguments.of(inputWithNoFields, expectedWithNoFields),
        Arguments.of(inputWithOneRequest, expectedWithOneRequest),
        Arguments.of(inputWithMultipleRequests, expectedWithMultipleRequests)
    );
  }

  @ParameterizedTest
  @MethodSource("provideQuantityInputs")
  void testMapQuantity(io.fabric8.kubernetes.api.model.Quantity input, Quantity expected) {
    Quantity actual = armadaMapper.mapQuantity(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideQuantityInputs() {
    io.fabric8.kubernetes.api.model.Quantity quantityWithAmount = new io.fabric8.kubernetes.api.model.Quantity();
    quantityWithAmount.setAmount("10");
    quantityWithAmount.setFormat("Gi");

    io.fabric8.kubernetes.api.model.Quantity quantityWithAmountOnly = new io.fabric8.kubernetes.api.model.Quantity();
    quantityWithAmountOnly.setAmount("10");

    io.fabric8.kubernetes.api.model.Quantity quantityWithNoFields = new io.fabric8.kubernetes.api.model.Quantity();

    Quantity expectedWithAmount = Quantity.newBuilder().setString("10Gi").build();
    Quantity expectedWithAmountOnly = Quantity.newBuilder().setString("10").build();
    Quantity expectedWithNoFields = Quantity.newBuilder().build();

    return Stream.of(
        Arguments.of(quantityWithAmount, expectedWithAmount),
        Arguments.of(quantityWithAmountOnly, expectedWithAmountOnly),
        Arguments.of(quantityWithNoFields, expectedWithNoFields)
    );
  }

  @ParameterizedTest
  @MethodSource("provideResourceRequirementsInputs")
  void testMapResourceRequirements(io.fabric8.kubernetes.api.model.ResourceRequirements input,
      Generated.ResourceRequirements expected) {
    Generated.ResourceRequirements actual = armadaMapper.mapResourceRequirements(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideResourceRequirementsInputs() {
    io.fabric8.kubernetes.api.model.ResourceRequirements resourceRequirementsWithLimits = new io.fabric8.kubernetes.api.model.ResourceRequirements();
    io.fabric8.kubernetes.api.model.Quantity quantity = new io.fabric8.kubernetes.api.model.Quantity();
    quantity.setAmount("10");
    quantity.setFormat("Gi");
    Map<String, io.fabric8.kubernetes.api.model.Quantity> limits = new HashMap<>();
    limits.put("cpu", quantity);
    resourceRequirementsWithLimits.setLimits(limits);

    io.fabric8.kubernetes.api.model.ResourceRequirements resourceRequirementsWithRequests = new io.fabric8.kubernetes.api.model.ResourceRequirements();
    Map<String, io.fabric8.kubernetes.api.model.Quantity> requests = new HashMap<>();
    requests.put("memory", quantity);
    resourceRequirementsWithRequests.setRequests(requests);

    io.fabric8.kubernetes.api.model.ResourceRequirements resourceRequirementsWithLimitsAndRequests = new io.fabric8.kubernetes.api.model.ResourceRequirements();
    resourceRequirementsWithLimitsAndRequests.setLimits(limits);
    resourceRequirementsWithLimitsAndRequests.setRequests(requests);

    Generated.ResourceRequirements expectedWithLimits = Generated.ResourceRequirements.newBuilder()
        .putAllLimits(Map.of("cpu", Quantity.newBuilder().setString("10Gi").build()))
        .build();

    Generated.ResourceRequirements expectedWithRequests = Generated.ResourceRequirements.newBuilder()
        .putAllRequests(Map.of("memory", Quantity.newBuilder().setString("10Gi").build()))
        .build();

    Generated.ResourceRequirements expectedWithLimitsAndRequests = Generated.ResourceRequirements.newBuilder()
        .putAllLimits(Map.of("cpu", Quantity.newBuilder().setString("10Gi").build()))
        .putAllRequests(Map.of("memory", Quantity.newBuilder().setString("10Gi").build()))
        .build();

    return Stream.of(
        Arguments.of(resourceRequirementsWithLimits, expectedWithLimits),
        Arguments.of(resourceRequirementsWithRequests, expectedWithRequests),
        Arguments.of(resourceRequirementsWithLimitsAndRequests, expectedWithLimitsAndRequests)
    );
  }

  @ParameterizedTest
  @MethodSource("provideContainerResizePolicyInputs")
  void testMapContainerResizePolicy(
      List<io.fabric8.kubernetes.api.model.ContainerResizePolicy> input,
      Iterable<? extends Generated.ContainerResizePolicy> expected) {
    Iterable<? extends Generated.ContainerResizePolicy> actual = armadaMapper.mapContainerResizePolicy(
        input);

    assertIterableEquals(expected, actual);
  }

  static Stream<Arguments> provideContainerResizePolicyInputs() {
    io.fabric8.kubernetes.api.model.ContainerResizePolicy policyWithResourceName = new io.fabric8.kubernetes.api.model.ContainerResizePolicy();
    policyWithResourceName.setResourceName("cpu");

    io.fabric8.kubernetes.api.model.ContainerResizePolicy policyWithRestartPolicy = new io.fabric8.kubernetes.api.model.ContainerResizePolicy();
    policyWithRestartPolicy.setRestartPolicy("Always");

    io.fabric8.kubernetes.api.model.ContainerResizePolicy policyWithBoth = new io.fabric8.kubernetes.api.model.ContainerResizePolicy();
    policyWithBoth.setResourceName("memory");
    policyWithBoth.setRestartPolicy("OnFailure");

    Generated.ContainerResizePolicy expectedWithResourceName = Generated.ContainerResizePolicy.newBuilder()
        .setResourceName("cpu")
        .build();

    Generated.ContainerResizePolicy expectedWithRestartPolicy = Generated.ContainerResizePolicy.newBuilder()
        .setRestartPolicy("Always")
        .build();

    Generated.ContainerResizePolicy expectedWithBoth = Generated.ContainerResizePolicy.newBuilder()
        .setResourceName("memory")
        .setRestartPolicy("OnFailure")
        .build();

    return Stream.of(
        Arguments.of(List.of(policyWithResourceName), List.of(expectedWithResourceName)),
        Arguments.of(List.of(policyWithRestartPolicy), List.of(expectedWithRestartPolicy)),
        Arguments.of(List.of(policyWithBoth), List.of(expectedWithBoth))
    );
  }

  @ParameterizedTest
  @MethodSource("provideVolumeMountsInputs")
  void testMapVolumeMounts(List<io.fabric8.kubernetes.api.model.VolumeMount> input,
      Iterable<? extends Generated.VolumeMount> expected) {
    Iterable<? extends Generated.VolumeMount> actual = armadaMapper.mapVolumeMounts(input);

    assertIterableEquals(expected, actual);
  }

  static Stream<Arguments> provideVolumeMountsInputs() {
    io.fabric8.kubernetes.api.model.VolumeMount mountWithName = new io.fabric8.kubernetes.api.model.VolumeMount();
    mountWithName.setName("volume1");

    io.fabric8.kubernetes.api.model.VolumeMount mountWithReadOnly = new io.fabric8.kubernetes.api.model.VolumeMount();
    mountWithReadOnly.setReadOnly(true);

    io.fabric8.kubernetes.api.model.VolumeMount mountWithMountPath = new io.fabric8.kubernetes.api.model.VolumeMount();
    mountWithMountPath.setMountPath("/mnt/data");

    io.fabric8.kubernetes.api.model.VolumeMount mountWithSubPath = new io.fabric8.kubernetes.api.model.VolumeMount();
    mountWithSubPath.setSubPath("subpath");

    io.fabric8.kubernetes.api.model.VolumeMount mountWithMountPropagation = new io.fabric8.kubernetes.api.model.VolumeMount();
    mountWithMountPropagation.setMountPropagation("Bidirectional");

    io.fabric8.kubernetes.api.model.VolumeMount mountWithSubPathExpr = new io.fabric8.kubernetes.api.model.VolumeMount();
    mountWithSubPathExpr.setSubPathExpr("subpathexpr");

    io.fabric8.kubernetes.api.model.VolumeMount mountWithAllFields = new io.fabric8.kubernetes.api.model.VolumeMount();
    mountWithAllFields.setName("volume2");
    mountWithAllFields.setMountPath("/mnt/data");
    mountWithAllFields.setSubPath("subpath");
    mountWithAllFields.setMountPropagation("Bidirectional");

    Generated.VolumeMount expectedWithName = Generated.VolumeMount.newBuilder()
        .setName("volume1")
        .build();

    Generated.VolumeMount expectedWithReadOnly = Generated.VolumeMount.newBuilder()
        .setReadOnly(true)
        .build();

    Generated.VolumeMount expectedWithMountPath = Generated.VolumeMount.newBuilder()
        .setMountPath("/mnt/data")
        .build();

    Generated.VolumeMount expectedWithSubPath = Generated.VolumeMount.newBuilder()
        .setSubPath("subpath")
        .build();

    Generated.VolumeMount expectedWithMountPropagation = Generated.VolumeMount.newBuilder()
        .setMountPropagation("Bidirectional")
        .build();

    Generated.VolumeMount expectedWithSubPathExpr = Generated.VolumeMount.newBuilder()
        .setSubPathExpr("subpathexpr")
        .build();

    Generated.VolumeMount expectedWithAllFields = Generated.VolumeMount.newBuilder()
        .setName("volume2")
        .setMountPath("/mnt/data")
        .setSubPath("subpath")
        .setMountPropagation("Bidirectional")
        .build();

    return Stream.of(
        Arguments.of(List.of(mountWithName), List.of(expectedWithName)),
        Arguments.of(List.of(mountWithReadOnly), List.of(expectedWithReadOnly)),
        Arguments.of(List.of(mountWithMountPath), List.of(expectedWithMountPath)),
        Arguments.of(List.of(mountWithSubPath), List.of(expectedWithSubPath)),
        Arguments.of(List.of(mountWithMountPropagation), List.of(expectedWithMountPropagation)),
        Arguments.of(List.of(mountWithSubPathExpr), List.of(expectedWithSubPathExpr)),
        Arguments.of(List.of(mountWithAllFields), List.of(expectedWithAllFields))
    );
  }

  @ParameterizedTest
  @MethodSource("provideVolumeDevicesInputs")
  void testMapVolumeDevices(List<io.fabric8.kubernetes.api.model.VolumeDevice> input,
      Iterable<? extends Generated.VolumeDevice> expected) {
    Iterable<? extends Generated.VolumeDevice> actual = armadaMapper.mapVolumeDevices(input);

    assertIterableEquals(expected, actual);
  }

  static Stream<Arguments> provideVolumeDevicesInputs() {
    io.fabric8.kubernetes.api.model.VolumeDevice deviceWithName = new io.fabric8.kubernetes.api.model.VolumeDevice();
    deviceWithName.setName("device1");

    io.fabric8.kubernetes.api.model.VolumeDevice deviceWithDevicePath = new io.fabric8.kubernetes.api.model.VolumeDevice();
    deviceWithDevicePath.setDevicePath("/dev/sda");

    io.fabric8.kubernetes.api.model.VolumeDevice deviceWithAllFields = new io.fabric8.kubernetes.api.model.VolumeDevice();
    deviceWithAllFields.setName("device2");
    deviceWithAllFields.setDevicePath("/dev/sdb");

    Generated.VolumeDevice expectedWithName = Generated.VolumeDevice.newBuilder()
        .setName("device1")
        .build();

    Generated.VolumeDevice expectedWithDevicePath = Generated.VolumeDevice.newBuilder()
        .setDevicePath("/dev/sda")
        .build();

    Generated.VolumeDevice expectedWithAllFields = Generated.VolumeDevice.newBuilder()
        .setName("device2")
        .setDevicePath("/dev/sdb")
        .build();

    return Stream.of(
        Arguments.of(List.of(deviceWithName), List.of(expectedWithName)),
        Arguments.of(List.of(deviceWithDevicePath), List.of(expectedWithDevicePath)),
        Arguments.of(List.of(deviceWithAllFields), List.of(expectedWithAllFields))
    );
  }

  @ParameterizedTest
  @MethodSource("provideIntOrStringInputs")
  void testMapIntOrString(io.fabric8.kubernetes.api.model.IntOrString input, IntOrString expected) {
    IntOrString actual = armadaMapper.mapIntOrString(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideIntOrStringInputs() {
    io.fabric8.kubernetes.api.model.IntOrString intPort = new io.fabric8.kubernetes.api.model.IntOrString();
    intPort.setValue(8080);

    io.fabric8.kubernetes.api.model.IntOrString strPort = new io.fabric8.kubernetes.api.model.IntOrString();
    strPort.setValue("http");

    io.fabric8.kubernetes.api.model.IntOrString emptyPort = new io.fabric8.kubernetes.api.model.IntOrString();

    IntOrString expectedIntPort = IntOrString.newBuilder()
        .setIntVal(8080)
        .build();

    IntOrString expectedStrPort = IntOrString.newBuilder()
        .setStrVal("http")
        .build();

    IntOrString expectedEmptyPort = IntOrString.newBuilder().build();

    return Stream.of(
        Arguments.of(intPort, expectedIntPort),
        Arguments.of(strPort, expectedStrPort),
        Arguments.of(emptyPort, expectedEmptyPort)
    );
  }

  @ParameterizedTest
  @MethodSource("provideHttpHeaderInputs")
  void testMapHttpHeaders(io.fabric8.kubernetes.api.model.HTTPHeader input,
      Generated.HTTPHeader expected) {
    Generated.HTTPHeader actual = armadaMapper.mapHttpHeaders(Collections.singletonList(input))
        .iterator().next();

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideHttpHeaderInputs() {
    io.fabric8.kubernetes.api.model.HTTPHeader headerWithNameAndValue = new io.fabric8.kubernetes.api.model.HTTPHeader();
    headerWithNameAndValue.setName("X-Header");
    headerWithNameAndValue.setValue("Value");

    io.fabric8.kubernetes.api.model.HTTPHeader headerWithNameOnly = new io.fabric8.kubernetes.api.model.HTTPHeader();
    headerWithNameOnly.setName("X-Header");

    io.fabric8.kubernetes.api.model.HTTPHeader headerWithValueOnly = new io.fabric8.kubernetes.api.model.HTTPHeader();
    headerWithValueOnly.setValue("Value");

    io.fabric8.kubernetes.api.model.HTTPHeader emptyHeader = new io.fabric8.kubernetes.api.model.HTTPHeader();

    Generated.HTTPHeader expectedHeaderWithNameAndValue = Generated.HTTPHeader.newBuilder()
        .setName("X-Header")
        .setValue("Value")
        .build();

    Generated.HTTPHeader expectedHeaderWithNameOnly = Generated.HTTPHeader.newBuilder()
        .setName("X-Header")
        .build();

    Generated.HTTPHeader expectedHeaderWithValueOnly = Generated.HTTPHeader.newBuilder()
        .setValue("Value")
        .build();

    Generated.HTTPHeader expectedEmptyHeader = Generated.HTTPHeader.newBuilder().build();

    return Stream.of(
        Arguments.of(headerWithNameAndValue, expectedHeaderWithNameAndValue),
        Arguments.of(headerWithNameOnly, expectedHeaderWithNameOnly),
        Arguments.of(headerWithValueOnly, expectedHeaderWithValueOnly),
        Arguments.of(emptyHeader, expectedEmptyHeader)
    );
  }

  @ParameterizedTest
  @MethodSource("provideHttpGetActionInputs")
  void testMapHttpGetAction(io.fabric8.kubernetes.api.model.HTTPGetAction input,
      Generated.HTTPGetAction expected) {
    Generated.HTTPGetAction actual = armadaMapper.mapHttpGetAction(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideHttpGetActionInputs() {
    io.fabric8.kubernetes.api.model.HTTPGetAction httpGetWithAllFields = new io.fabric8.kubernetes.api.model.HTTPGetAction();
    httpGetWithAllFields.setPath("/test");
    httpGetWithAllFields.setPort(new io.fabric8.kubernetes.api.model.IntOrString("8080"));
    httpGetWithAllFields.setHost("localhost");
    httpGetWithAllFields.setScheme("HTTP");
    httpGetWithAllFields.setHttpHeaders(Collections.singletonList(
        new io.fabric8.kubernetes.api.model.HTTPHeader("X-Custom-Header", "Value")));

    io.fabric8.kubernetes.api.model.HTTPGetAction httpGetWithPathOnly = new io.fabric8.kubernetes.api.model.HTTPGetAction();
    httpGetWithPathOnly.setPath("/test");

    io.fabric8.kubernetes.api.model.HTTPGetAction httpGetWithPortOnly = new io.fabric8.kubernetes.api.model.HTTPGetAction();
    httpGetWithPortOnly.setPort(new io.fabric8.kubernetes.api.model.IntOrString("8080"));

    io.fabric8.kubernetes.api.model.HTTPGetAction httpGetWithHostOnly = new io.fabric8.kubernetes.api.model.HTTPGetAction();
    httpGetWithHostOnly.setHost("localhost");

    io.fabric8.kubernetes.api.model.HTTPGetAction httpGetWithSchemeOnly = new io.fabric8.kubernetes.api.model.HTTPGetAction();
    httpGetWithSchemeOnly.setScheme("HTTP");

    io.fabric8.kubernetes.api.model.HTTPGetAction httpGetWithHttpHeadersOnly = new io.fabric8.kubernetes.api.model.HTTPGetAction();
    httpGetWithHttpHeadersOnly.setHttpHeaders(Collections.singletonList(
        new io.fabric8.kubernetes.api.model.HTTPHeader("X-Custom-Header", "Value")));

    io.fabric8.kubernetes.api.model.HTTPGetAction httpGetWithEmptyFields = new io.fabric8.kubernetes.api.model.HTTPGetAction();

    Generated.HTTPGetAction expectedHttpGetWithAllFields = Generated.HTTPGetAction.newBuilder()
        .setPath("/test")
        .setPort(IntOrString.newBuilder().setStrVal("8080").build())
        .setHost("localhost")
        .setScheme("HTTP")
        .addAllHttpHeaders(Collections.singletonList(
            Generated.HTTPHeader.newBuilder().setName("X-Custom-Header").setValue("Value").build()))
        .build();

    Generated.HTTPGetAction expectedHttpGetWithPathOnly = Generated.HTTPGetAction.newBuilder()
        .setPath("/test")
        .build();

    Generated.HTTPGetAction expectedHttpGetWithPortOnly = Generated.HTTPGetAction.newBuilder()
        .setPort(IntOrString.newBuilder().setStrVal("8080").build())
        .build();

    Generated.HTTPGetAction expectedHttpGetWithHostOnly = Generated.HTTPGetAction.newBuilder()
        .setHost("localhost")
        .build();

    Generated.HTTPGetAction expectedHttpGetWithSchemeOnly = Generated.HTTPGetAction.newBuilder()
        .setScheme("HTTP")
        .build();

    Generated.HTTPGetAction expectedHttpGetWithHttpHeadersOnly = Generated.HTTPGetAction.newBuilder()
        .addAllHttpHeaders(Collections.singletonList(
            Generated.HTTPHeader.newBuilder().setName("X-Custom-Header").setValue("Value").build()))
        .build();

    Generated.HTTPGetAction expectedHttpGetWithEmptyFields = Generated.HTTPGetAction.newBuilder()
        .build();

    return Stream.of(
        Arguments.of(httpGetWithAllFields, expectedHttpGetWithAllFields),
        Arguments.of(httpGetWithPathOnly, expectedHttpGetWithPathOnly),
        Arguments.of(httpGetWithPortOnly, expectedHttpGetWithPortOnly),
        Arguments.of(httpGetWithHostOnly, expectedHttpGetWithHostOnly),
        Arguments.of(httpGetWithSchemeOnly, expectedHttpGetWithSchemeOnly),
        Arguments.of(httpGetWithHttpHeadersOnly, expectedHttpGetWithHttpHeadersOnly),
        Arguments.of(httpGetWithEmptyFields, expectedHttpGetWithEmptyFields)
    );
  }

  @ParameterizedTest
  @MethodSource("provideTcpSocketActionInputs")
  void testMapTCPSocketAction(io.fabric8.kubernetes.api.model.TCPSocketAction input,
      Generated.TCPSocketAction expected) {
    Generated.TCPSocketAction actual = armadaMapper.mapTCPSocketAction(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideTcpSocketActionInputs() {
    io.fabric8.kubernetes.api.model.TCPSocketAction tcpSocketWithAllFields = new io.fabric8.kubernetes.api.model.TCPSocketAction();
    tcpSocketWithAllFields.setPort(new io.fabric8.kubernetes.api.model.IntOrString("8080"));
    tcpSocketWithAllFields.setHost("localhost");

    io.fabric8.kubernetes.api.model.TCPSocketAction tcpSocketWithPortOnly = new io.fabric8.kubernetes.api.model.TCPSocketAction();
    tcpSocketWithPortOnly.setPort(new io.fabric8.kubernetes.api.model.IntOrString("8080"));

    io.fabric8.kubernetes.api.model.TCPSocketAction tcpSocketWithHostOnly = new io.fabric8.kubernetes.api.model.TCPSocketAction();
    tcpSocketWithHostOnly.setHost("localhost");

    io.fabric8.kubernetes.api.model.TCPSocketAction tcpSocketWithEmptyFields = new io.fabric8.kubernetes.api.model.TCPSocketAction();

    Generated.TCPSocketAction expectedTcpSocketWithAllFields = Generated.TCPSocketAction.newBuilder()
        .setPort(IntOrString.newBuilder().setStrVal("8080").build())
        .setHost("localhost")
        .build();

    Generated.TCPSocketAction expectedTcpSocketWithPortOnly = Generated.TCPSocketAction.newBuilder()
        .setPort(IntOrString.newBuilder().setStrVal("8080").build())
        .build();

    Generated.TCPSocketAction expectedTcpSocketWithHostOnly = Generated.TCPSocketAction.newBuilder()
        .setHost("localhost")
        .build();

    Generated.TCPSocketAction expectedTcpSocketWithEmptyFields = Generated.TCPSocketAction.newBuilder()
        .build();

    return Stream.of(
        Arguments.of(tcpSocketWithAllFields, expectedTcpSocketWithAllFields),
        Arguments.of(tcpSocketWithPortOnly, expectedTcpSocketWithPortOnly),
        Arguments.of(tcpSocketWithHostOnly, expectedTcpSocketWithHostOnly),
        Arguments.of(tcpSocketWithEmptyFields, expectedTcpSocketWithEmptyFields)
    );
  }

  @ParameterizedTest
  @MethodSource("provideGrpcActionInputs")
  void testMapGRPCAction(io.fabric8.kubernetes.api.model.GRPCAction input,
      Generated.GRPCAction expected) {
    Generated.GRPCAction actual = armadaMapper.mapGRPCAction(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideGrpcActionInputs() {
    io.fabric8.kubernetes.api.model.GRPCAction grpcWithAllFields = new io.fabric8.kubernetes.api.model.GRPCAction();
    grpcWithAllFields.setPort(9090);
    grpcWithAllFields.setService("my-service");

    io.fabric8.kubernetes.api.model.GRPCAction grpcWithPortOnly = new io.fabric8.kubernetes.api.model.GRPCAction();
    grpcWithPortOnly.setPort(9090);

    io.fabric8.kubernetes.api.model.GRPCAction grpcWithServiceOnly = new io.fabric8.kubernetes.api.model.GRPCAction();
    grpcWithServiceOnly.setService("my-service");

    io.fabric8.kubernetes.api.model.GRPCAction grpcWithEmptyFields = new io.fabric8.kubernetes.api.model.GRPCAction();

    Generated.GRPCAction expectedGrpcWithAllFields = Generated.GRPCAction.newBuilder()
        .setPort(9090)
        .setService("my-service")
        .build();

    Generated.GRPCAction expectedGrpcWithPortOnly = Generated.GRPCAction.newBuilder()
        .setPort(9090)
        .build();

    Generated.GRPCAction expectedGrpcWithServiceOnly = Generated.GRPCAction.newBuilder()
        .setService("my-service")
        .build();

    Generated.GRPCAction expectedGrpcWithEmptyFields = Generated.GRPCAction.newBuilder().build();

    return Stream.of(
        Arguments.of(grpcWithAllFields, expectedGrpcWithAllFields),
        Arguments.of(grpcWithPortOnly, expectedGrpcWithPortOnly),
        Arguments.of(grpcWithServiceOnly, expectedGrpcWithServiceOnly),
        Arguments.of(grpcWithEmptyFields, expectedGrpcWithEmptyFields)
    );
  }

  @ParameterizedTest
  @MethodSource("provideProbeHandlerInputs")
  void testMapProbeHandler(io.fabric8.kubernetes.api.model.Probe input,
      Generated.ProbeHandler expected) {
    Generated.ProbeHandler actual = armadaMapper.mapProbeHandler(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideProbeHandlerInputs() {
    io.fabric8.kubernetes.api.model.Probe probeWithAllFields = new io.fabric8.kubernetes.api.model.Probe();
    probeWithAllFields.setExec(new io.fabric8.kubernetes.api.model.ExecAction());
    probeWithAllFields.setHttpGet(new io.fabric8.kubernetes.api.model.HTTPGetAction());
    probeWithAllFields.setTcpSocket(new io.fabric8.kubernetes.api.model.TCPSocketAction());
    probeWithAllFields.setGrpc(new io.fabric8.kubernetes.api.model.GRPCAction());

    io.fabric8.kubernetes.api.model.Probe probeWithExecOnly = new io.fabric8.kubernetes.api.model.Probe();
    probeWithExecOnly.setExec(new io.fabric8.kubernetes.api.model.ExecAction());

    io.fabric8.kubernetes.api.model.Probe probeWithHttpGetOnly = new io.fabric8.kubernetes.api.model.Probe();
    probeWithHttpGetOnly.setHttpGet(new io.fabric8.kubernetes.api.model.HTTPGetAction());

    io.fabric8.kubernetes.api.model.Probe probeWithTcpSocketOnly = new io.fabric8.kubernetes.api.model.Probe();
    probeWithTcpSocketOnly.setTcpSocket(new io.fabric8.kubernetes.api.model.TCPSocketAction());

    io.fabric8.kubernetes.api.model.Probe probeWithGrpcOnly = new io.fabric8.kubernetes.api.model.Probe();
    probeWithGrpcOnly.setGrpc(new io.fabric8.kubernetes.api.model.GRPCAction());

    io.fabric8.kubernetes.api.model.Probe probeWithEmptyFields = new io.fabric8.kubernetes.api.model.Probe();

    Generated.ProbeHandler expectedProbeWithAllFields = Generated.ProbeHandler.newBuilder()
        .setExec(Generated.ExecAction.newBuilder().build())
        .setHttpGet(Generated.HTTPGetAction.newBuilder().build())
        .setTcpSocket(Generated.TCPSocketAction.newBuilder().build())
        .setGrpc(Generated.GRPCAction.newBuilder().build())
        .build();

    Generated.ProbeHandler expectedProbeWithExecOnly = Generated.ProbeHandler.newBuilder()
        .setExec(Generated.ExecAction.newBuilder().build())
        .build();

    Generated.ProbeHandler expectedProbeWithHttpGetOnly = Generated.ProbeHandler.newBuilder()
        .setHttpGet(Generated.HTTPGetAction.newBuilder().build())
        .build();

    Generated.ProbeHandler expectedProbeWithTcpSocketOnly = Generated.ProbeHandler.newBuilder()
        .setTcpSocket(Generated.TCPSocketAction.newBuilder().build())
        .build();

    Generated.ProbeHandler expectedProbeWithGrpcOnly = Generated.ProbeHandler.newBuilder()
        .setGrpc(Generated.GRPCAction.newBuilder().build())
        .build();

    Generated.ProbeHandler expectedProbeWithEmptyFields = Generated.ProbeHandler.newBuilder()
        .build();

    return Stream.of(
        Arguments.of(probeWithAllFields, expectedProbeWithAllFields),
        Arguments.of(probeWithExecOnly, expectedProbeWithExecOnly),
        Arguments.of(probeWithHttpGetOnly, expectedProbeWithHttpGetOnly),
        Arguments.of(probeWithTcpSocketOnly, expectedProbeWithTcpSocketOnly),
        Arguments.of(probeWithGrpcOnly, expectedProbeWithGrpcOnly),
        Arguments.of(probeWithEmptyFields, expectedProbeWithEmptyFields)
    );
  }

  @ParameterizedTest
  @MethodSource("provideExecActionInputs")
  void testMapExecAction(io.fabric8.kubernetes.api.model.ExecAction input,
      Generated.ExecAction expected) {
    Generated.ExecAction actual = armadaMapper.mapExecAction(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideExecActionInputs() {
    io.fabric8.kubernetes.api.model.ExecAction execWithCommand = new io.fabric8.kubernetes.api.model.ExecAction();
    execWithCommand.setCommand(Arrays.asList("command1", "command2"));

    io.fabric8.kubernetes.api.model.ExecAction execWithEmptyCommand = new io.fabric8.kubernetes.api.model.ExecAction();

    Generated.ExecAction expectedExecWithCommand = Generated.ExecAction.newBuilder()
        .addAllCommand(Arrays.asList("command1", "command2"))
        .build();

    Generated.ExecAction expectedExecWithEmptyCommand = Generated.ExecAction.newBuilder().build();

    return Stream.of(
        Arguments.of(execWithCommand, expectedExecWithCommand),
        Arguments.of(execWithEmptyCommand, expectedExecWithEmptyCommand)
    );
  }

  @ParameterizedTest
  @MethodSource("provideProbeInputs")
  void testMapProbe(io.fabric8.kubernetes.api.model.Probe input, Generated.Probe expected) {
    Generated.Probe actual = armadaMapper.mapProbe(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideProbeInputs() {
    io.fabric8.kubernetes.api.model.Probe probeWithHandler = new io.fabric8.kubernetes.api.model.Probe();

    io.fabric8.kubernetes.api.model.Probe probeWithInitialDelay = new io.fabric8.kubernetes.api.model.Probe();
    probeWithInitialDelay.setInitialDelaySeconds(10);

    io.fabric8.kubernetes.api.model.Probe probeWithTimeout = new io.fabric8.kubernetes.api.model.Probe();
    probeWithTimeout.setTimeoutSeconds(5);

    io.fabric8.kubernetes.api.model.Probe probeWithPeriod = new io.fabric8.kubernetes.api.model.Probe();
    probeWithPeriod.setPeriodSeconds(15);

    io.fabric8.kubernetes.api.model.Probe probeWithSuccessThreshold = new io.fabric8.kubernetes.api.model.Probe();
    probeWithSuccessThreshold.setSuccessThreshold(1);

    io.fabric8.kubernetes.api.model.Probe probeWithFailureThreshold = new io.fabric8.kubernetes.api.model.Probe();
    probeWithFailureThreshold.setFailureThreshold(3);

    io.fabric8.kubernetes.api.model.Probe probeWithTerminationGracePeriod = new io.fabric8.kubernetes.api.model.Probe();
    probeWithTerminationGracePeriod.setTerminationGracePeriodSeconds(30L);

    io.fabric8.kubernetes.api.model.Probe probeWithAllFields = new io.fabric8.kubernetes.api.model.Probe();
    probeWithAllFields.setInitialDelaySeconds(10);
    probeWithAllFields.setTimeoutSeconds(5);
    probeWithAllFields.setPeriodSeconds(15);
    probeWithAllFields.setSuccessThreshold(1);
    probeWithAllFields.setFailureThreshold(3);
    probeWithAllFields.setTerminationGracePeriodSeconds(30L);

    io.fabric8.kubernetes.api.model.Probe probeWithNoFields = new io.fabric8.kubernetes.api.model.Probe();

    Generated.Probe expectedWithHandler = Generated.Probe.newBuilder()
        .setHandler(Generated.ProbeHandler.newBuilder().build())
        .build();

    Generated.Probe expectedWithInitialDelay = Generated.Probe.newBuilder()
        .setHandler(Generated.ProbeHandler.newBuilder().build())
        .setInitialDelaySeconds(10)
        .build();

    Generated.Probe expectedWithTimeout = Generated.Probe.newBuilder()
        .setHandler(Generated.ProbeHandler.newBuilder().build())
        .setTimeoutSeconds(5)
        .build();

    Generated.Probe expectedWithPeriod = Generated.Probe.newBuilder()
        .setHandler(Generated.ProbeHandler.newBuilder().build())
        .setPeriodSeconds(15)
        .build();

    Generated.Probe expectedWithSuccessThreshold = Generated.Probe.newBuilder()
        .setHandler(Generated.ProbeHandler.newBuilder().build())
        .setSuccessThreshold(1)
        .build();

    Generated.Probe expectedWithFailureThreshold = Generated.Probe.newBuilder()
        .setHandler(Generated.ProbeHandler.newBuilder().build())
        .setFailureThreshold(3)
        .build();

    Generated.Probe expectedWithTerminationGracePeriod = Generated.Probe.newBuilder()
        .setHandler(Generated.ProbeHandler.newBuilder().build())
        .setTerminationGracePeriodSeconds(30)
        .build();

    Generated.Probe expectedWithAllFields = Generated.Probe.newBuilder()
        .setHandler(Generated.ProbeHandler.newBuilder().build())
        .setHandler(Generated.ProbeHandler.newBuilder().build())
        .setInitialDelaySeconds(10)
        .setTimeoutSeconds(5)
        .setPeriodSeconds(15)
        .setSuccessThreshold(1)
        .setFailureThreshold(3)
        .setTerminationGracePeriodSeconds(30)
        .build();

    Generated.Probe expectedWithNoFields = Generated.Probe.newBuilder()
        .setHandler(Generated.ProbeHandler.newBuilder().build())
        .build();

    return Stream.of(
        Arguments.of(probeWithHandler, expectedWithHandler),
        Arguments.of(probeWithInitialDelay, expectedWithInitialDelay),
        Arguments.of(probeWithTimeout, expectedWithTimeout),
        Arguments.of(probeWithPeriod, expectedWithPeriod),
        Arguments.of(probeWithSuccessThreshold, expectedWithSuccessThreshold),
        Arguments.of(probeWithFailureThreshold, expectedWithFailureThreshold),
        Arguments.of(probeWithTerminationGracePeriod, expectedWithTerminationGracePeriod),
        Arguments.of(probeWithAllFields, expectedWithAllFields),
        Arguments.of(probeWithNoFields, expectedWithNoFields)
    );
  }

  @ParameterizedTest
  @MethodSource("provideLifecycleHandlerInputs")
  void testMapLifecycleHandler(io.fabric8.kubernetes.api.model.LifecycleHandler input,
      Generated.LifecycleHandler expected) {
    Generated.LifecycleHandler actual = armadaMapper.mapLifecycleHandler(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideLifecycleHandlerInputs() {
    io.fabric8.kubernetes.api.model.LifecycleHandler lifecycleWithExec = new io.fabric8.kubernetes.api.model.LifecycleHandler();
    lifecycleWithExec.setExec(new io.fabric8.kubernetes.api.model.ExecAction());
    lifecycleWithExec.getExec().setCommand(Arrays.asList("command1", "command2"));

    io.fabric8.kubernetes.api.model.LifecycleHandler lifecycleWithHttpGet = new io.fabric8.kubernetes.api.model.LifecycleHandler();
    lifecycleWithHttpGet.setHttpGet(new io.fabric8.kubernetes.api.model.HTTPGetAction());

    io.fabric8.kubernetes.api.model.LifecycleHandler lifecycleWithTcpSocket = new io.fabric8.kubernetes.api.model.LifecycleHandler();
    lifecycleWithTcpSocket.setTcpSocket(new io.fabric8.kubernetes.api.model.TCPSocketAction());

    io.fabric8.kubernetes.api.model.LifecycleHandler lifecycleWithSleep = new io.fabric8.kubernetes.api.model.LifecycleHandler();
    lifecycleWithSleep.setSleep(new io.fabric8.kubernetes.api.model.SleepAction());
    lifecycleWithSleep.getSleep().setSeconds(10L);

    io.fabric8.kubernetes.api.model.LifecycleHandler lifecycleWithAllFields = new io.fabric8.kubernetes.api.model.LifecycleHandler();
    lifecycleWithAllFields.setExec(new io.fabric8.kubernetes.api.model.ExecAction());
    lifecycleWithAllFields.getExec().setCommand(Arrays.asList("command1", "command2"));
    lifecycleWithAllFields.setHttpGet(new io.fabric8.kubernetes.api.model.HTTPGetAction());
    lifecycleWithAllFields.setTcpSocket(new io.fabric8.kubernetes.api.model.TCPSocketAction());
    lifecycleWithAllFields.setSleep(new io.fabric8.kubernetes.api.model.SleepAction());
    lifecycleWithAllFields.getSleep().setSeconds(10L);

    io.fabric8.kubernetes.api.model.LifecycleHandler lifecycleWithNoFields = new io.fabric8.kubernetes.api.model.LifecycleHandler();

    Generated.LifecycleHandler expectedWithExec = Generated.LifecycleHandler.newBuilder()
        .setExec(
            Generated.ExecAction.newBuilder().addAllCommand(Arrays.asList("command1", "command2"))
                .build())
        .build();

    Generated.LifecycleHandler expectedWithHttpGet = Generated.LifecycleHandler.newBuilder()
        .setHttpGet(Generated.HTTPGetAction.newBuilder().build())
        .build();

    Generated.LifecycleHandler expectedWithTcpSocket = Generated.LifecycleHandler.newBuilder()
        .setTcpSocket(Generated.TCPSocketAction.newBuilder().build())
        .build();

    Generated.LifecycleHandler expectedWithSleep = Generated.LifecycleHandler.newBuilder()
        .setSleep(Generated.SleepAction.newBuilder().setSeconds(10).build())
        .build();

    Generated.LifecycleHandler expectedWithAllFields = Generated.LifecycleHandler.newBuilder()
        .setExec(
            Generated.ExecAction.newBuilder().addAllCommand(Arrays.asList("command1", "command2"))
                .build())
        .setHttpGet(Generated.HTTPGetAction.newBuilder().build())
        .setTcpSocket(Generated.TCPSocketAction.newBuilder().build())
        .setSleep(Generated.SleepAction.newBuilder().setSeconds(10).build())
        .build();

    Generated.LifecycleHandler expectedWithNoFields = Generated.LifecycleHandler.newBuilder()
        .build();

    return Stream.of(
        Arguments.of(lifecycleWithExec, expectedWithExec),
        Arguments.of(lifecycleWithHttpGet, expectedWithHttpGet),
        Arguments.of(lifecycleWithTcpSocket, expectedWithTcpSocket),
        Arguments.of(lifecycleWithSleep, expectedWithSleep),
        Arguments.of(lifecycleWithAllFields, expectedWithAllFields),
        Arguments.of(lifecycleWithNoFields, expectedWithNoFields)
    );
  }

  @ParameterizedTest
  @MethodSource("provideLifecycleInputs")
  void testMapLifecycle(io.fabric8.kubernetes.api.model.Lifecycle input,
      Generated.Lifecycle expected) {
    Generated.Lifecycle actual = armadaMapper.mapLifecycle(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideLifecycleInputs() {
    io.fabric8.kubernetes.api.model.Lifecycle lifecycleWithPostStart = new io.fabric8.kubernetes.api.model.Lifecycle();
    lifecycleWithPostStart.setPostStart(new io.fabric8.kubernetes.api.model.LifecycleHandler());

    io.fabric8.kubernetes.api.model.Lifecycle lifecycleWithPreStop = new io.fabric8.kubernetes.api.model.Lifecycle();
    lifecycleWithPreStop.setPreStop(new io.fabric8.kubernetes.api.model.LifecycleHandler());

    io.fabric8.kubernetes.api.model.Lifecycle lifecycleWithBothHandlers = new io.fabric8.kubernetes.api.model.Lifecycle();
    lifecycleWithBothHandlers.setPostStart(new io.fabric8.kubernetes.api.model.LifecycleHandler());
    lifecycleWithBothHandlers.setPreStop(new io.fabric8.kubernetes.api.model.LifecycleHandler());

    io.fabric8.kubernetes.api.model.Lifecycle lifecycleWithNoHandlers = new io.fabric8.kubernetes.api.model.Lifecycle();

    Generated.Lifecycle expectedWithPostStart = Generated.Lifecycle.newBuilder()
        .setPostStart(Generated.LifecycleHandler.newBuilder().build())
        .build();

    Generated.Lifecycle expectedWithPreStop = Generated.Lifecycle.newBuilder()
        .setPreStop(Generated.LifecycleHandler.newBuilder().build())
        .build();

    Generated.Lifecycle expectedWithBothHandlers = Generated.Lifecycle.newBuilder()
        .setPostStart(Generated.LifecycleHandler.newBuilder().build())
        .setPreStop(Generated.LifecycleHandler.newBuilder().build())
        .build();

    Generated.Lifecycle expectedWithNoHandlers = Generated.Lifecycle.newBuilder().build();

    return Stream.of(
        Arguments.of(lifecycleWithPostStart, expectedWithPostStart),
        Arguments.of(lifecycleWithPreStop, expectedWithPreStop),
        Arguments.of(lifecycleWithBothHandlers, expectedWithBothHandlers),
        Arguments.of(lifecycleWithNoHandlers, expectedWithNoHandlers)
    );
  }

  @ParameterizedTest
  @MethodSource("provideCapabilitiesInputs")
  void testMapCapabilities(io.fabric8.kubernetes.api.model.Capabilities input,
      Generated.Capabilities expected) {
    Generated.Capabilities actual = armadaMapper.mapCapabilities(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideCapabilitiesInputs() {
    io.fabric8.kubernetes.api.model.Capabilities capabilitiesWithDrop = new io.fabric8.kubernetes.api.model.Capabilities();
    capabilitiesWithDrop.setDrop(List.of("CAP_NET_ADMIN"));

    io.fabric8.kubernetes.api.model.Capabilities capabilitiesWithAdd = new io.fabric8.kubernetes.api.model.Capabilities();
    capabilitiesWithAdd.setAdd(List.of("CAP_SYS_ADMIN"));

    io.fabric8.kubernetes.api.model.Capabilities capabilitiesWithBoth = new io.fabric8.kubernetes.api.model.Capabilities();
    capabilitiesWithBoth.setDrop(List.of("CAP_NET_ADMIN"));
    capabilitiesWithBoth.setAdd(List.of("CAP_SYS_ADMIN"));

    io.fabric8.kubernetes.api.model.Capabilities capabilitiesWithNoFields = new io.fabric8.kubernetes.api.model.Capabilities();

    Generated.Capabilities expectedWithDrop = Generated.Capabilities.newBuilder()
        .addAllDrop(List.of("CAP_NET_ADMIN"))
        .build();

    Generated.Capabilities expectedWithAdd = Generated.Capabilities.newBuilder()
        .addAllAdd(List.of("CAP_SYS_ADMIN"))
        .build();

    Generated.Capabilities expectedWithBoth = Generated.Capabilities.newBuilder()
        .addAllDrop(List.of("CAP_NET_ADMIN"))
        .addAllAdd(List.of("CAP_SYS_ADMIN"))
        .build();

    Generated.Capabilities expectedWithNoFields = Generated.Capabilities.newBuilder().build();

    return Stream.of(
        Arguments.of(capabilitiesWithDrop, expectedWithDrop),
        Arguments.of(capabilitiesWithAdd, expectedWithAdd),
        Arguments.of(capabilitiesWithBoth, expectedWithBoth),
        Arguments.of(capabilitiesWithNoFields, expectedWithNoFields)
    );
  }

  @ParameterizedTest
  @MethodSource("provideSeLinuxOptionsInputs")
  void testMapSeLinuxOptions(io.fabric8.kubernetes.api.model.SELinuxOptions input,
      Generated.SELinuxOptions expected) {
    Generated.SELinuxOptions actual = armadaMapper.mapSeLinuxOptions(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideSeLinuxOptionsInputs() {
    io.fabric8.kubernetes.api.model.SELinuxOptions seLinuxOptionsWithUser = new io.fabric8.kubernetes.api.model.SELinuxOptions();
    seLinuxOptionsWithUser.setUser("user");

    io.fabric8.kubernetes.api.model.SELinuxOptions seLinuxOptionsWithRole = new io.fabric8.kubernetes.api.model.SELinuxOptions();
    seLinuxOptionsWithRole.setRole("role");

    io.fabric8.kubernetes.api.model.SELinuxOptions seLinuxOptionsWithType = new io.fabric8.kubernetes.api.model.SELinuxOptions();
    seLinuxOptionsWithType.setType("type");

    io.fabric8.kubernetes.api.model.SELinuxOptions seLinuxOptionsWithLevel = new io.fabric8.kubernetes.api.model.SELinuxOptions();
    seLinuxOptionsWithLevel.setLevel("level");

    io.fabric8.kubernetes.api.model.SELinuxOptions seLinuxOptionsWithAllFields = new io.fabric8.kubernetes.api.model.SELinuxOptions();
    seLinuxOptionsWithAllFields.setUser("user");
    seLinuxOptionsWithAllFields.setRole("role");
    seLinuxOptionsWithAllFields.setType("type");
    seLinuxOptionsWithAllFields.setLevel("level");

    io.fabric8.kubernetes.api.model.SELinuxOptions seLinuxOptionsWithNoFields = new io.fabric8.kubernetes.api.model.SELinuxOptions();

    Generated.SELinuxOptions expectedWithUser = Generated.SELinuxOptions.newBuilder()
        .setUser("user")
        .build();

    Generated.SELinuxOptions expectedWithRole = Generated.SELinuxOptions.newBuilder()
        .setRole("role")
        .build();

    Generated.SELinuxOptions expectedWithType = Generated.SELinuxOptions.newBuilder()
        .setType("type")
        .build();

    Generated.SELinuxOptions expectedWithLevel = Generated.SELinuxOptions.newBuilder()
        .setLevel("level")
        .build();

    Generated.SELinuxOptions expectedWithAllFields = Generated.SELinuxOptions.newBuilder()
        .setUser("user")
        .setRole("role")
        .setType("type")
        .setLevel("level")
        .build();

    Generated.SELinuxOptions expectedWithNoFields = Generated.SELinuxOptions.newBuilder().build();

    return Stream.of(
        Arguments.of(seLinuxOptionsWithUser, expectedWithUser),
        Arguments.of(seLinuxOptionsWithRole, expectedWithRole),
        Arguments.of(seLinuxOptionsWithType, expectedWithType),
        Arguments.of(seLinuxOptionsWithLevel, expectedWithLevel),
        Arguments.of(seLinuxOptionsWithAllFields, expectedWithAllFields),
        Arguments.of(seLinuxOptionsWithNoFields, expectedWithNoFields)
    );
  }

  @ParameterizedTest
  @MethodSource("provideWindowsOptionsInputs")
  void testMapWindowsOptions(io.fabric8.kubernetes.api.model.WindowsSecurityContextOptions input,
      Generated.WindowsSecurityContextOptions expected) {
    Generated.WindowsSecurityContextOptions actual = armadaMapper.mapWindowsOptions(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideWindowsOptionsInputs() {
    io.fabric8.kubernetes.api.model.WindowsSecurityContextOptions windowsOptionsWithGmsaCredentialSpec = new io.fabric8.kubernetes.api.model.WindowsSecurityContextOptions();
    windowsOptionsWithGmsaCredentialSpec.setGmsaCredentialSpec("gmsaSpec");

    io.fabric8.kubernetes.api.model.WindowsSecurityContextOptions windowsOptionsWithGmsaCredentialSpecName = new io.fabric8.kubernetes.api.model.WindowsSecurityContextOptions();
    windowsOptionsWithGmsaCredentialSpecName.setGmsaCredentialSpecName("gmsaSpecName");

    io.fabric8.kubernetes.api.model.WindowsSecurityContextOptions windowsOptionsWithRunAsUserName = new io.fabric8.kubernetes.api.model.WindowsSecurityContextOptions();
    windowsOptionsWithRunAsUserName.setRunAsUserName("user");

    io.fabric8.kubernetes.api.model.WindowsSecurityContextOptions windowsOptionsWithHostProcess = new io.fabric8.kubernetes.api.model.WindowsSecurityContextOptions();
    windowsOptionsWithHostProcess.setHostProcess(true);

    io.fabric8.kubernetes.api.model.WindowsSecurityContextOptions windowsOptionsWithAllFields = new io.fabric8.kubernetes.api.model.WindowsSecurityContextOptions();
    windowsOptionsWithAllFields.setGmsaCredentialSpec("gmsaSpec");
    windowsOptionsWithAllFields.setGmsaCredentialSpecName("gmsaSpecName");
    windowsOptionsWithAllFields.setRunAsUserName("user");
    windowsOptionsWithAllFields.setHostProcess(true);

    io.fabric8.kubernetes.api.model.WindowsSecurityContextOptions windowsOptionsWithNoFields = new io.fabric8.kubernetes.api.model.WindowsSecurityContextOptions();

    Generated.WindowsSecurityContextOptions expectedWithGmsaCredentialSpec = Generated.WindowsSecurityContextOptions.newBuilder()
        .setGmsaCredentialSpec("gmsaSpec")
        .build();

    Generated.WindowsSecurityContextOptions expectedWithGmsaCredentialSpecName = Generated.WindowsSecurityContextOptions.newBuilder()
        .setGmsaCredentialSpecName("gmsaSpecName")
        .build();

    Generated.WindowsSecurityContextOptions expectedWithRunAsUserName = Generated.WindowsSecurityContextOptions.newBuilder()
        .setRunAsUserName("user")
        .build();

    Generated.WindowsSecurityContextOptions expectedWithHostProcess = Generated.WindowsSecurityContextOptions.newBuilder()
        .setHostProcess(true)
        .build();

    Generated.WindowsSecurityContextOptions expectedWithAllFields = Generated.WindowsSecurityContextOptions.newBuilder()
        .setGmsaCredentialSpec("gmsaSpec")
        .setGmsaCredentialSpecName("gmsaSpecName")
        .setRunAsUserName("user")
        .setHostProcess(true)
        .build();

    Generated.WindowsSecurityContextOptions expectedWithNoFields = Generated.WindowsSecurityContextOptions.newBuilder()
        .build();

    return Stream.of(
        Arguments.of(windowsOptionsWithGmsaCredentialSpec, expectedWithGmsaCredentialSpec),
        Arguments.of(windowsOptionsWithGmsaCredentialSpecName, expectedWithGmsaCredentialSpecName),
        Arguments.of(windowsOptionsWithRunAsUserName, expectedWithRunAsUserName),
        Arguments.of(windowsOptionsWithHostProcess, expectedWithHostProcess),
        Arguments.of(windowsOptionsWithAllFields, expectedWithAllFields),
        Arguments.of(windowsOptionsWithNoFields, expectedWithNoFields)
    );
  }

  @ParameterizedTest
  @MethodSource("provideSeccompProfileInputs")
  void testMapSeccompProfile(io.fabric8.kubernetes.api.model.SeccompProfile input,
      Generated.SeccompProfile expected) {
    Generated.SeccompProfile actual = armadaMapper.mapSeccompProfile(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideSeccompProfileInputs() {
    io.fabric8.kubernetes.api.model.SeccompProfile seccompProfileWithType = new io.fabric8.kubernetes.api.model.SeccompProfile();
    seccompProfileWithType.setType("RuntimeDefault");

    io.fabric8.kubernetes.api.model.SeccompProfile seccompProfileWithLocalhostProfile = new io.fabric8.kubernetes.api.model.SeccompProfile();
    seccompProfileWithLocalhostProfile.setLocalhostProfile("localhostProfile");

    io.fabric8.kubernetes.api.model.SeccompProfile seccompProfileWithTypeAndLocalhostProfile = new io.fabric8.kubernetes.api.model.SeccompProfile();
    seccompProfileWithTypeAndLocalhostProfile.setType("RuntimeDefault");
    seccompProfileWithTypeAndLocalhostProfile.setLocalhostProfile("localhostProfile");

    io.fabric8.kubernetes.api.model.SeccompProfile seccompProfileWithNoFields = new io.fabric8.kubernetes.api.model.SeccompProfile();

    Generated.SeccompProfile expectedWithType = Generated.SeccompProfile.newBuilder()
        .setType("RuntimeDefault")
        .build();

    Generated.SeccompProfile expectedWithLocalhostProfile = Generated.SeccompProfile.newBuilder()
        .setLocalhostProfile("localhostProfile")
        .build();

    Generated.SeccompProfile expectedWithTypeAndLocalhostProfile = Generated.SeccompProfile.newBuilder()
        .setType("RuntimeDefault")
        .setLocalhostProfile("localhostProfile")
        .build();

    Generated.SeccompProfile expectedWithNoFields = Generated.SeccompProfile.newBuilder().build();

    return Stream.of(
        Arguments.of(seccompProfileWithType, expectedWithType),
        Arguments.of(seccompProfileWithLocalhostProfile, expectedWithLocalhostProfile),
        Arguments.of(seccompProfileWithTypeAndLocalhostProfile,
            expectedWithTypeAndLocalhostProfile),
        Arguments.of(seccompProfileWithNoFields, expectedWithNoFields)
    );
  }

  @ParameterizedTest
  @MethodSource("provideSecurityContextInputs")
  void testMapSecurityContext(io.fabric8.kubernetes.api.model.SecurityContext input,
      Generated.SecurityContext expected) {
    Generated.SecurityContext actual = armadaMapper.mapSecurityContext(input);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> provideSecurityContextInputs() {
    io.fabric8.kubernetes.api.model.SecurityContext securityContextWithCapabilities = new io.fabric8.kubernetes.api.model.SecurityContext();
    securityContextWithCapabilities.setCapabilities(
        new io.fabric8.kubernetes.api.model.Capabilities());

    io.fabric8.kubernetes.api.model.SecurityContext securityContextWithPrivileged = new io.fabric8.kubernetes.api.model.SecurityContext();
    securityContextWithPrivileged.setPrivileged(true);

    io.fabric8.kubernetes.api.model.SecurityContext securityContextWithSeLinuxOptions = new io.fabric8.kubernetes.api.model.SecurityContext();
    securityContextWithSeLinuxOptions.setSeLinuxOptions(
        new io.fabric8.kubernetes.api.model.SELinuxOptions());

    io.fabric8.kubernetes.api.model.SecurityContext securityContextWithWindowsOptions = new io.fabric8.kubernetes.api.model.SecurityContext();
    securityContextWithWindowsOptions.setWindowsOptions(
        new io.fabric8.kubernetes.api.model.WindowsSecurityContextOptions());

    io.fabric8.kubernetes.api.model.SecurityContext securityContextWithRunAsUser = new io.fabric8.kubernetes.api.model.SecurityContext();
    securityContextWithRunAsUser.setRunAsUser(1000L);

    io.fabric8.kubernetes.api.model.SecurityContext securityContextWithRunAsGroup = new io.fabric8.kubernetes.api.model.SecurityContext();
    securityContextWithRunAsGroup.setRunAsGroup(2000L);

    io.fabric8.kubernetes.api.model.SecurityContext securityContextWithRunAsNonRoot = new io.fabric8.kubernetes.api.model.SecurityContext();
    securityContextWithRunAsNonRoot.setRunAsNonRoot(true);

    io.fabric8.kubernetes.api.model.SecurityContext securityContextWithReadOnlyRootFilesystem = new io.fabric8.kubernetes.api.model.SecurityContext();
    securityContextWithReadOnlyRootFilesystem.setReadOnlyRootFilesystem(true);

    io.fabric8.kubernetes.api.model.SecurityContext securityContextWithAllowPrivilegeEscalation = new io.fabric8.kubernetes.api.model.SecurityContext();
    securityContextWithAllowPrivilegeEscalation.setAllowPrivilegeEscalation(true);

    io.fabric8.kubernetes.api.model.SecurityContext securityContextWithProcMount = new io.fabric8.kubernetes.api.model.SecurityContext();
    securityContextWithProcMount.setProcMount("default");

    io.fabric8.kubernetes.api.model.SecurityContext securityContextWithSeccompProfile = new io.fabric8.kubernetes.api.model.SecurityContext();
    securityContextWithSeccompProfile.setSeccompProfile(
        new io.fabric8.kubernetes.api.model.SeccompProfile());

    io.fabric8.kubernetes.api.model.SecurityContext securityContextWithAllFields = new io.fabric8.kubernetes.api.model.SecurityContext();
    securityContextWithAllFields.setCapabilities(
        new io.fabric8.kubernetes.api.model.Capabilities());
    securityContextWithAllFields.setPrivileged(true);
    securityContextWithAllFields.setSeLinuxOptions(
        new io.fabric8.kubernetes.api.model.SELinuxOptions());
    securityContextWithAllFields.setWindowsOptions(
        new io.fabric8.kubernetes.api.model.WindowsSecurityContextOptions());
    securityContextWithAllFields.setRunAsUser(1000L);
    securityContextWithAllFields.setRunAsGroup(2000L);
    securityContextWithAllFields.setRunAsNonRoot(true);
    securityContextWithAllFields.setReadOnlyRootFilesystem(true);
    securityContextWithAllFields.setAllowPrivilegeEscalation(true);
    securityContextWithAllFields.setProcMount("default");
    securityContextWithAllFields.setSeccompProfile(
        new io.fabric8.kubernetes.api.model.SeccompProfile());

    io.fabric8.kubernetes.api.model.SecurityContext securityContextWithNoFields = new io.fabric8.kubernetes.api.model.SecurityContext();

    Generated.SecurityContext expectedWithCapabilities = Generated.SecurityContext.newBuilder()
        .setCapabilities(Generated.Capabilities.newBuilder().build())
        .build();

    Generated.SecurityContext expectedWithPrivileged = Generated.SecurityContext.newBuilder()
        .setPrivileged(true)
        .build();

    Generated.SecurityContext expectedWithSeLinuxOptions = Generated.SecurityContext.newBuilder()
        .setSeLinuxOptions(Generated.SELinuxOptions.newBuilder().build())
        .build();

    Generated.SecurityContext expectedWithWindowsOptions = Generated.SecurityContext.newBuilder()
        .setWindowsOptions(Generated.WindowsSecurityContextOptions.newBuilder().build())
        .build();

    Generated.SecurityContext expectedWithRunAsUser = Generated.SecurityContext.newBuilder()
        .setRunAsUser(1000L)
        .build();

    Generated.SecurityContext expectedWithRunAsGroup = Generated.SecurityContext.newBuilder()
        .setRunAsGroup(2000L)
        .build();

    Generated.SecurityContext expectedWithRunAsNonRoot = Generated.SecurityContext.newBuilder()
        .setRunAsNonRoot(true)
        .build();

    Generated.SecurityContext expectedWithReadOnlyRootFilesystem = Generated.SecurityContext.newBuilder()
        .setReadOnlyRootFilesystem(true)
        .build();

    Generated.SecurityContext expectedWithAllowPrivilegeEscalation = Generated.SecurityContext.newBuilder()
        .setAllowPrivilegeEscalation(true)
        .build();

    Generated.SecurityContext expectedWithProcMount = Generated.SecurityContext.newBuilder()
        .setProcMount("default")
        .build();

    Generated.SecurityContext expectedWithSeccompProfile = Generated.SecurityContext.newBuilder()
        .setSeccompProfile(Generated.SeccompProfile.newBuilder().build())
        .build();

    Generated.SecurityContext expectedWithAllFields = Generated.SecurityContext.newBuilder()
        .setCapabilities(Generated.Capabilities.newBuilder().build())
        .setPrivileged(true)
        .setSeLinuxOptions(Generated.SELinuxOptions.newBuilder().build())
        .setWindowsOptions(Generated.WindowsSecurityContextOptions.newBuilder().build())
        .setRunAsUser(1000L)
        .setRunAsGroup(2000L)
        .setRunAsNonRoot(true)
        .setReadOnlyRootFilesystem(true)
        .setAllowPrivilegeEscalation(true)
        .setProcMount("default")
        .setSeccompProfile(Generated.SeccompProfile.newBuilder().build())
        .build();

    Generated.SecurityContext expectedWithNoFields = Generated.SecurityContext.newBuilder().build();

    return Stream.of(
        Arguments.of(securityContextWithCapabilities, expectedWithCapabilities),
        Arguments.of(securityContextWithPrivileged, expectedWithPrivileged),
        Arguments.of(securityContextWithSeLinuxOptions, expectedWithSeLinuxOptions),
        Arguments.of(securityContextWithWindowsOptions, expectedWithWindowsOptions),
        Arguments.of(securityContextWithRunAsUser, expectedWithRunAsUser),
        Arguments.of(securityContextWithRunAsGroup, expectedWithRunAsGroup),
        Arguments.of(securityContextWithRunAsNonRoot, expectedWithRunAsNonRoot),
        Arguments.of(securityContextWithReadOnlyRootFilesystem, expectedWithReadOnlyRootFilesystem),
        Arguments.of(securityContextWithAllowPrivilegeEscalation,
            expectedWithAllowPrivilegeEscalation),
        Arguments.of(securityContextWithProcMount, expectedWithProcMount),
        Arguments.of(securityContextWithSeccompProfile, expectedWithSeccompProfile),
        Arguments.of(securityContextWithAllFields, expectedWithAllFields),
        Arguments.of(securityContextWithNoFields, expectedWithNoFields)
    );
  }

  @ParameterizedTest
  @MethodSource("provideContainersInputs")
  void testMapContainers(List<io.fabric8.kubernetes.api.model.Container> input,
      Iterable<? extends Generated.Container> expected) {
    Iterable<? extends Generated.Container> actual = armadaMapper.mapContainers(input);

    assertIterableEquals(expected, actual);
  }

  static Stream<Arguments> provideContainersInputs() {
    io.fabric8.kubernetes.api.model.Container containerWithName = new io.fabric8.kubernetes.api.model.Container();
    containerWithName.setName("container1");

    io.fabric8.kubernetes.api.model.Container containerWithImage = new io.fabric8.kubernetes.api.model.Container();
    containerWithImage.setImage("my-image");

    io.fabric8.kubernetes.api.model.Container containerWithCommand = new io.fabric8.kubernetes.api.model.Container();
    containerWithCommand.setCommand(Arrays.asList("start", "server"));

    io.fabric8.kubernetes.api.model.Container containerWithArgs = new io.fabric8.kubernetes.api.model.Container();
    containerWithArgs.setArgs(Arrays.asList("--port", "8080"));

    io.fabric8.kubernetes.api.model.Container containerWithWorkingDir = new io.fabric8.kubernetes.api.model.Container();
    containerWithWorkingDir.setWorkingDir("/app");

    io.fabric8.kubernetes.api.model.Container containerWithPorts = new io.fabric8.kubernetes.api.model.Container();
    containerWithPorts.setPorts(List.of(new ContainerPort()));

    io.fabric8.kubernetes.api.model.Container containerWithEnvFrom = new io.fabric8.kubernetes.api.model.Container();
    containerWithEnvFrom.setEnvFrom(List.of(new io.fabric8.kubernetes.api.model.EnvFromSource()));

    io.fabric8.kubernetes.api.model.Container containerWithEnv = new io.fabric8.kubernetes.api.model.Container();
    containerWithEnv.setEnv(List.of(new io.fabric8.kubernetes.api.model.EnvVar()));

    io.fabric8.kubernetes.api.model.Container containerWithResources = new io.fabric8.kubernetes.api.model.Container();
    containerWithResources.setResources(new io.fabric8.kubernetes.api.model.ResourceRequirements());

    io.fabric8.kubernetes.api.model.Container containerWithResizePolicy = new io.fabric8.kubernetes.api.model.Container();
    containerWithResizePolicy.setResizePolicy(List.of(new ContainerResizePolicy()));

    io.fabric8.kubernetes.api.model.Container containerWithRestartPolicy = new io.fabric8.kubernetes.api.model.Container();
    containerWithRestartPolicy.setRestartPolicy("Always");

    io.fabric8.kubernetes.api.model.Container containerWithVolumeMounts = new io.fabric8.kubernetes.api.model.Container();
    containerWithVolumeMounts.setVolumeMounts(List.of(new VolumeMount()));

    io.fabric8.kubernetes.api.model.Container containerWithVolumeDevices = new io.fabric8.kubernetes.api.model.Container();
    containerWithVolumeDevices.setVolumeDevices(List.of(new VolumeDevice()));

    io.fabric8.kubernetes.api.model.Container containerWithLivenessProbe = new io.fabric8.kubernetes.api.model.Container();
    containerWithLivenessProbe.setLivenessProbe(new io.fabric8.kubernetes.api.model.Probe());

    io.fabric8.kubernetes.api.model.Container containerWithReadinessProbe = new io.fabric8.kubernetes.api.model.Container();
    containerWithReadinessProbe.setReadinessProbe(new io.fabric8.kubernetes.api.model.Probe());

    io.fabric8.kubernetes.api.model.Container containerWithStartupProbe = new io.fabric8.kubernetes.api.model.Container();
    containerWithStartupProbe.setStartupProbe(new io.fabric8.kubernetes.api.model.Probe());

    io.fabric8.kubernetes.api.model.Container containerWithLifecycle = new io.fabric8.kubernetes.api.model.Container();
    containerWithLifecycle.setLifecycle(new io.fabric8.kubernetes.api.model.Lifecycle());

    io.fabric8.kubernetes.api.model.Container containerWithTerminationMessagePath = new io.fabric8.kubernetes.api.model.Container();
    containerWithTerminationMessagePath.setTerminationMessagePath("/tmp/term_message");

    io.fabric8.kubernetes.api.model.Container containerWithTerminationMessagePolicy = new io.fabric8.kubernetes.api.model.Container();
    containerWithTerminationMessagePolicy.setTerminationMessagePolicy("FallbackToLogsOnError");

    io.fabric8.kubernetes.api.model.Container containerWithImagePullPolicy = new io.fabric8.kubernetes.api.model.Container();
    containerWithImagePullPolicy.setImagePullPolicy("IfNotPresent");

    io.fabric8.kubernetes.api.model.Container containerWithSecurityContext = new io.fabric8.kubernetes.api.model.Container();
    containerWithSecurityContext.setSecurityContext(
        new io.fabric8.kubernetes.api.model.SecurityContext());

    io.fabric8.kubernetes.api.model.Container containerWithStdin = new io.fabric8.kubernetes.api.model.Container();
    containerWithStdin.setStdin(true);

    io.fabric8.kubernetes.api.model.Container containerWithStdinOnce = new io.fabric8.kubernetes.api.model.Container();
    containerWithStdinOnce.setStdinOnce(true);

    io.fabric8.kubernetes.api.model.Container containerWithTty = new io.fabric8.kubernetes.api.model.Container();
    containerWithTty.setTty(true);

    io.fabric8.kubernetes.api.model.Container containerWithAllFields = new io.fabric8.kubernetes.api.model.Container();
    containerWithAllFields.setName("container1");
    containerWithAllFields.setImage("my-image");
    containerWithAllFields.setCommand(Arrays.asList("start", "server"));
    containerWithAllFields.setArgs(Arrays.asList("--port", "8080"));
    containerWithAllFields.setWorkingDir("/app");
    containerWithAllFields.setPorts(List.of(new ContainerPort()));
    containerWithAllFields.setEnvFrom(List.of(new io.fabric8.kubernetes.api.model.EnvFromSource()));
    containerWithAllFields.setEnv(List.of(new io.fabric8.kubernetes.api.model.EnvVar()));
    containerWithAllFields.setResources(new io.fabric8.kubernetes.api.model.ResourceRequirements());
    containerWithAllFields.setResizePolicy(List.of(new ContainerResizePolicy()));
    containerWithAllFields.setRestartPolicy("Always");
    containerWithAllFields.setVolumeMounts(List.of(new VolumeMount()));
    containerWithAllFields.setVolumeDevices(List.of(new VolumeDevice()));
    containerWithAllFields.setLivenessProbe(new io.fabric8.kubernetes.api.model.Probe());
    containerWithAllFields.setReadinessProbe(new io.fabric8.kubernetes.api.model.Probe());
    containerWithAllFields.setStartupProbe(new io.fabric8.kubernetes.api.model.Probe());
    containerWithAllFields.setLifecycle(new io.fabric8.kubernetes.api.model.Lifecycle());
    containerWithAllFields.setTerminationMessagePath("/tmp/term_message");
    containerWithAllFields.setTerminationMessagePolicy("FallbackToLogsOnError");
    containerWithAllFields.setImagePullPolicy("IfNotPresent");
    containerWithAllFields.setSecurityContext(
        new io.fabric8.kubernetes.api.model.SecurityContext());
    containerWithAllFields.setStdin(true);
    containerWithAllFields.setStdinOnce(true);
    containerWithAllFields.setTty(true);

    io.fabric8.kubernetes.api.model.Container containerWithNoFields = new io.fabric8.kubernetes.api.model.Container();

    Generated.Container expectedWithName = Generated.Container.newBuilder()
        .setName("container1")
        .build();

    Generated.Container expectedWithImage = Generated.Container.newBuilder()
        .setImage("my-image")
        .build();

    Generated.Container expectedWithCommand = Generated.Container.newBuilder()
        .addAllCommand(Arrays.asList("start", "server"))
        .build();

    Generated.Container expectedWithArgs = Generated.Container.newBuilder()
        .addAllArgs(Arrays.asList("--port", "8080"))
        .build();

    Generated.Container expectedWithWorkingDir = Generated.Container.newBuilder()
        .setWorkingDir("/app")
        .build();

    Generated.Container expectedWithPorts = Generated.Container.newBuilder()
        .addAllPorts(List.of(Generated.ContainerPort.newBuilder().build()))
        .build();

    Generated.Container expectedWithEnvFrom = Container.newBuilder()
        .addAllEnvFrom(List.of(EnvFromSource.newBuilder().build()))
        .build();

    Generated.Container expectedWithEnv = Container.newBuilder()
        .addAllEnv(List.of(EnvVar.newBuilder().build()))
        .build();

    Generated.Container expectedWithResources = Generated.Container.newBuilder()
        .setResources(Generated.ResourceRequirements.newBuilder().build())
        .build();

    Generated.Container expectedWithResizePolicy = Container.newBuilder()
        .addAllResizePolicy(List.of(Generated.ContainerResizePolicy.newBuilder().build()))
        .build();

    Generated.Container expectedWithRestartPolicy = Generated.Container.newBuilder()
        .setRestartPolicy("Always")
        .build();

    Generated.Container expectedWithVolumeMounts = Container.newBuilder()
        .addAllVolumeMounts(List.of(Generated.VolumeMount.newBuilder().build()))
        .build();

    Generated.Container expectedWithVolumeDevices = Container.newBuilder()
        .addAllVolumeDevices(List.of(Generated.VolumeDevice.newBuilder().build()))
        .build();

    Generated.Container expectedWithLivenessProbe = Generated.Container.newBuilder()
        .setLivenessProbe(Generated.Probe.newBuilder()
            .setHandler(Generated.ProbeHandler.newBuilder().build())
            .build())
        .build();

    Generated.Container expectedWithReadinessProbe = Generated.Container.newBuilder()
        .setReadinessProbe(Generated.Probe.newBuilder()
            .setHandler(Generated.ProbeHandler.newBuilder().build())
            .build())
        .build();

    Generated.Container expectedWithStartupProbe = Generated.Container.newBuilder()
        .setStartupProbe(Generated.Probe.newBuilder()
            .setHandler(Generated.ProbeHandler.newBuilder().build())
            .build())
        .build();

    Generated.Container expectedWithLifecycle = Generated.Container.newBuilder()
        .setLifecycle(Generated.Lifecycle.newBuilder().build())
        .build();

    Generated.Container expectedWithTerminationMessagePath = Generated.Container.newBuilder()
        .setTerminationMessagePath("/tmp/term_message")
        .build();

    Generated.Container expectedWithTerminationMessagePolicy = Generated.Container.newBuilder()
        .setTerminationMessagePolicy("FallbackToLogsOnError")
        .build();

    Generated.Container expectedWithImagePullPolicy = Generated.Container.newBuilder()
        .setImagePullPolicy("IfNotPresent")
        .build();

    Generated.Container expectedWithSecurityContext = Generated.Container.newBuilder()
        .setSecurityContext(Generated.SecurityContext.newBuilder().build())
        .build();

    Generated.Container expectedWithStdin = Generated.Container.newBuilder()
        .setStdin(true)
        .build();

    Generated.Container expectedWithStdinOnce = Generated.Container.newBuilder()
        .setStdinOnce(true)
        .build();

    Generated.Container expectedWithTty = Generated.Container.newBuilder()
        .setTty(true)
        .build();

    Generated.Container expectedWithAllFields = Container.newBuilder()
        .setName("container1")
        .setImage("my-image")
        .addAllCommand(Arrays.asList("start", "server"))
        .addAllArgs(Arrays.asList("--port", "8080"))
        .setWorkingDir("/app")
        .addAllPorts(List.of(Generated.ContainerPort.newBuilder().build()))
        .addAllEnvFrom(List.of(EnvFromSource.newBuilder().build()))
        .addAllEnv(List.of(EnvVar.newBuilder().build()))
        .setResources(ResourceRequirements.newBuilder().build())
        .addAllResizePolicy(List.of(Generated.ContainerResizePolicy.newBuilder().build()))
        .setRestartPolicy("Always")
        .addAllVolumeMounts(List.of(Generated.VolumeMount.newBuilder().build()))
        .addAllVolumeDevices(List.of(Generated.VolumeDevice.newBuilder().build()))
        .setLivenessProbe(Probe.newBuilder()
            .setHandler(ProbeHandler.newBuilder().build())
            .build())
        .setReadinessProbe(Probe.newBuilder()
            .setHandler(ProbeHandler.newBuilder().build())
            .build())
        .setStartupProbe(Probe.newBuilder()
            .setHandler(ProbeHandler.newBuilder().build())
            .build())
        .setLifecycle(Lifecycle.newBuilder().build())
        .setTerminationMessagePath("/tmp/term_message")
        .setTerminationMessagePolicy("FallbackToLogsOnError")
        .setImagePullPolicy("IfNotPresent")
        .setSecurityContext(SecurityContext.newBuilder().build())
        .setStdin(true)
        .setStdinOnce(true)
        .setTty(true)
        .build();

    Generated.Container expectedWithNoFields = Generated.Container.newBuilder().build();

    return Stream.of(
        Arguments.of(Collections.singletonList(containerWithName),
            Collections.singletonList(expectedWithName)),
        Arguments.of(Collections.singletonList(containerWithImage),
            Collections.singletonList(expectedWithImage)),
        Arguments.of(Collections.singletonList(containerWithCommand),
            Collections.singletonList(expectedWithCommand)),
        Arguments.of(Collections.singletonList(containerWithArgs),
            Collections.singletonList(expectedWithArgs)),
        Arguments.of(Collections.singletonList(containerWithWorkingDir),
            Collections.singletonList(expectedWithWorkingDir)),
        Arguments.of(Collections.singletonList(containerWithPorts),
            Collections.singletonList(expectedWithPorts)),
        Arguments.of(Collections.singletonList(containerWithEnvFrom),
            Collections.singletonList(expectedWithEnvFrom)),
        Arguments.of(Collections.singletonList(containerWithEnv),
            Collections.singletonList(expectedWithEnv)),
        Arguments.of(Collections.singletonList(containerWithResources),
            Collections.singletonList(expectedWithResources)),
        Arguments.of(Collections.singletonList(containerWithResizePolicy),
            Collections.singletonList(expectedWithResizePolicy)),
        Arguments.of(Collections.singletonList(containerWithRestartPolicy),
            Collections.singletonList(expectedWithRestartPolicy)),
        Arguments.of(Collections.singletonList(containerWithVolumeMounts),
            Collections.singletonList(expectedWithVolumeMounts)),
        Arguments.of(Collections.singletonList(containerWithVolumeDevices),
            Collections.singletonList(expectedWithVolumeDevices)),
        Arguments.of(Collections.singletonList(containerWithLivenessProbe),
            Collections.singletonList(expectedWithLivenessProbe)),
        Arguments.of(Collections.singletonList(containerWithReadinessProbe),
            Collections.singletonList(expectedWithReadinessProbe)),
        Arguments.of(Collections.singletonList(containerWithStartupProbe),
            Collections.singletonList(expectedWithStartupProbe)),
        Arguments.of(Collections.singletonList(containerWithLifecycle),
            Collections.singletonList(expectedWithLifecycle)),
        Arguments.of(Collections.singletonList(containerWithTerminationMessagePath),
            Collections.singletonList(expectedWithTerminationMessagePath)),
        Arguments.of(Collections.singletonList(containerWithTerminationMessagePolicy),
            Collections.singletonList(expectedWithTerminationMessagePolicy)),
        Arguments.of(Collections.singletonList(containerWithImagePullPolicy),
            Collections.singletonList(expectedWithImagePullPolicy)),
        Arguments.of(Collections.singletonList(containerWithSecurityContext),
            Collections.singletonList(expectedWithSecurityContext)),
        Arguments.of(Collections.singletonList(containerWithStdin),
            Collections.singletonList(expectedWithStdin)),
        Arguments.of(Collections.singletonList(containerWithStdinOnce),
            Collections.singletonList(expectedWithStdinOnce)),
        Arguments.of(Collections.singletonList(containerWithTty),
            Collections.singletonList(expectedWithTty)),
        Arguments.of(Collections.singletonList(containerWithAllFields),
            Collections.singletonList(expectedWithAllFields)),
        Arguments.of(Collections.singletonList(containerWithNoFields),
            Collections.singletonList(expectedWithNoFields))
    );
  }

  @ParameterizedTest
  @MethodSource("provideSysctlInputs")
  void testMapSysctls(List<io.fabric8.kubernetes.api.model.Sysctl> input,
      List<Generated.Sysctl> expected) {
    Iterable<? extends Sysctl> result = armadaMapper.mapSysctls(input);

    assertEquals(expected, result);
  }

  static Stream<Arguments> provideSysctlInputs() {
    io.fabric8.kubernetes.api.model.Sysctl sysctlWithName = new io.fabric8.kubernetes.api.model.Sysctl();
    sysctlWithName.setName("net.core.somaxconn");

    io.fabric8.kubernetes.api.model.Sysctl sysctlWithValue = new io.fabric8.kubernetes.api.model.Sysctl();
    sysctlWithValue.setValue("1024");

    io.fabric8.kubernetes.api.model.Sysctl sysctlWithBoth = new io.fabric8.kubernetes.api.model.Sysctl();
    sysctlWithBoth.setName("net.core.rmem_max");
    sysctlWithBoth.setValue("2097152");

    Generated.Sysctl expectedWithName = Generated.Sysctl.newBuilder()
        .setName("net.core.somaxconn")
        .build();

    Generated.Sysctl expectedWithValue = Generated.Sysctl.newBuilder()
        .setValue("1024")
        .build();

    Generated.Sysctl expectedWithBoth = Generated.Sysctl.newBuilder()
        .setName("net.core.rmem_max")
        .setValue("2097152")
        .build();

    return Stream.of(
        Arguments.of(Collections.singletonList(sysctlWithName),
            Collections.singletonList(expectedWithName)),
        Arguments.of(Collections.singletonList(sysctlWithValue),
            Collections.singletonList(expectedWithValue)),
        Arguments.of(Collections.singletonList(sysctlWithBoth),
            Collections.singletonList(expectedWithBoth)),
        Arguments.of(Collections.emptyList(), Collections.emptyList())
    );
  }

  @ParameterizedTest
  @MethodSource("providePodSecurityContextInputs")
  void testMapPodSecurityContext(io.fabric8.kubernetes.api.model.PodSecurityContext input,
      Generated.PodSecurityContext expected) {
    Generated.PodSecurityContext result = armadaMapper.mapPodSecurityContext(input);

    assertEquals(expected, result);
  }

  static Stream<Arguments> providePodSecurityContextInputs() {
    io.fabric8.kubernetes.api.model.PodSecurityContext contextWithSeLinuxOptions = new io.fabric8.kubernetes.api.model.PodSecurityContext();
    contextWithSeLinuxOptions.setSeLinuxOptions(
        new io.fabric8.kubernetes.api.model.SELinuxOptions("level", "role", "type", "user"));

    Generated.PodSecurityContext expectedWithSeLinuxOptions = Generated.PodSecurityContext.newBuilder()
        .setSeLinuxOptions(Generated.SELinuxOptions.newBuilder()
            .setUser("user")
            .setRole("role")
            .setType("type")
            .setLevel("level")
            .build())
        .build();

    io.fabric8.kubernetes.api.model.PodSecurityContext contextWithWindowsOptions = new io.fabric8.kubernetes.api.model.PodSecurityContext();
    contextWithWindowsOptions.setWindowsOptions(
        new io.fabric8.kubernetes.api.model.WindowsSecurityContextOptions());

    Generated.PodSecurityContext expectedWithWindowsOptions = Generated.PodSecurityContext.newBuilder()
        .setWindowsOptions(Generated.WindowsSecurityContextOptions.newBuilder().build())
        .build();

    io.fabric8.kubernetes.api.model.PodSecurityContext contextWithRunAsUser = new io.fabric8.kubernetes.api.model.PodSecurityContext();
    contextWithRunAsUser.setRunAsUser(1000L);

    Generated.PodSecurityContext expectedWithRunAsUser = Generated.PodSecurityContext.newBuilder()
        .setRunAsUser(1000L)
        .build();

    io.fabric8.kubernetes.api.model.PodSecurityContext contextWithRunAsGroup = new io.fabric8.kubernetes.api.model.PodSecurityContext();
    contextWithRunAsGroup.setRunAsGroup(2000L);

    Generated.PodSecurityContext expectedWithRunAsGroup = Generated.PodSecurityContext.newBuilder()
        .setRunAsGroup(2000L)
        .build();

    io.fabric8.kubernetes.api.model.PodSecurityContext contextWithRunAsNonRoot = new io.fabric8.kubernetes.api.model.PodSecurityContext();
    contextWithRunAsNonRoot.setRunAsNonRoot(true);

    Generated.PodSecurityContext expectedWithRunAsNonRoot = Generated.PodSecurityContext.newBuilder()
        .setRunAsNonRoot(true)
        .build();

    io.fabric8.kubernetes.api.model.PodSecurityContext contextWithSupplementalGroups = new io.fabric8.kubernetes.api.model.PodSecurityContext();
    contextWithSupplementalGroups.setSupplementalGroups(Arrays.asList(3000L, 4000L));

    Generated.PodSecurityContext expectedWithSupplementalGroups = Generated.PodSecurityContext.newBuilder()
        .addAllSupplementalGroups(Arrays.asList(3000L, 4000L))
        .build();

    io.fabric8.kubernetes.api.model.PodSecurityContext contextWithFsGroup = new io.fabric8.kubernetes.api.model.PodSecurityContext();
    contextWithFsGroup.setFsGroup(5000L);

    Generated.PodSecurityContext expectedWithFsGroup = Generated.PodSecurityContext.newBuilder()
        .setFsGroup(5000L)
        .build();

    io.fabric8.kubernetes.api.model.PodSecurityContext contextWithSysctls = new io.fabric8.kubernetes.api.model.PodSecurityContext();
    contextWithSysctls.setSysctls(
        Arrays.asList(new io.fabric8.kubernetes.api.model.Sysctl("name1", "value1")));

    Generated.PodSecurityContext expectedWithSysctls = Generated.PodSecurityContext.newBuilder()
        .addAllSysctls(List.of(Sysctl.newBuilder()
            .setName("name1")
            .setValue("value1")
            .build()))
        .build();

    io.fabric8.kubernetes.api.model.PodSecurityContext contextWithFsGroupChangePolicy = new io.fabric8.kubernetes.api.model.PodSecurityContext();
    contextWithFsGroupChangePolicy.setFsGroupChangePolicy("OnRootMismatch");

    Generated.PodSecurityContext expectedWithFsGroupChangePolicy = Generated.PodSecurityContext.newBuilder()
        .setFsGroupChangePolicy("OnRootMismatch")
        .build();

    io.fabric8.kubernetes.api.model.PodSecurityContext contextWithSeccompProfile = new io.fabric8.kubernetes.api.model.PodSecurityContext();
    contextWithSeccompProfile.setSeccompProfile(
        new io.fabric8.kubernetes.api.model.SeccompProfile("RuntimeDefault", "type"));

    Generated.PodSecurityContext expectedWithSeccompProfile = Generated.PodSecurityContext.newBuilder()
        .setSeccompProfile(Generated.SeccompProfile.newBuilder()
            .setLocalhostProfile("RuntimeDefault")
            .setType("type")
            .build())
        .build();

    io.fabric8.kubernetes.api.model.PodSecurityContext contextWithAllFields = new io.fabric8.kubernetes.api.model.PodSecurityContext();
    contextWithAllFields.setSeLinuxOptions(
        new io.fabric8.kubernetes.api.model.SELinuxOptions("level", "role", "type", "user"));
    contextWithAllFields.setWindowsOptions(
        new io.fabric8.kubernetes.api.model.WindowsSecurityContextOptions());
    contextWithAllFields.setRunAsUser(1000L);
    contextWithAllFields.setRunAsGroup(2000L);
    contextWithAllFields.setRunAsNonRoot(true);
    contextWithAllFields.setSupplementalGroups(Arrays.asList(3000L, 4000L));
    contextWithAllFields.setFsGroup(5000L);
    contextWithAllFields.setSysctls(
        List.of(new io.fabric8.kubernetes.api.model.Sysctl("name1", "value1")));
    contextWithAllFields.setFsGroupChangePolicy("OnRootMismatch");
    contextWithAllFields.setSeccompProfile(
        new io.fabric8.kubernetes.api.model.SeccompProfile("RuntimeDefault", "type"));

    Generated.PodSecurityContext expectedWithAllFields = Generated.PodSecurityContext.newBuilder()
        .setSeLinuxOptions(Generated.SELinuxOptions.newBuilder()
            .setUser("user")
            .setRole("role")
            .setType("type")
            .setLevel("level")
            .build())
        .setWindowsOptions(Generated.WindowsSecurityContextOptions.newBuilder().build())
        .setRunAsUser(1000L)
        .setRunAsGroup(2000L)
        .setRunAsNonRoot(true)
        .addAllSupplementalGroups(Arrays.asList(3000L, 4000L))
        .setFsGroup(5000L)
        .addAllSysctls(List.of(Sysctl.newBuilder()
            .setName("name1")
            .setValue("value1")
            .build()))
        .setFsGroupChangePolicy("OnRootMismatch")
        .setSeccompProfile(Generated.SeccompProfile.newBuilder()
            .setLocalhostProfile("RuntimeDefault")
            .setType("type")
            .build())
        .build();

    io.fabric8.kubernetes.api.model.PodSecurityContext emptyContext = new io.fabric8.kubernetes.api.model.PodSecurityContext();
    Generated.PodSecurityContext expectedEmptyContext = Generated.PodSecurityContext.newBuilder()
        .build();

    return Stream.of(
        Arguments.of(contextWithSeLinuxOptions, expectedWithSeLinuxOptions),
        Arguments.of(contextWithWindowsOptions, expectedWithWindowsOptions),
        Arguments.of(contextWithRunAsUser, expectedWithRunAsUser),
        Arguments.of(contextWithRunAsGroup, expectedWithRunAsGroup),
        Arguments.of(contextWithRunAsNonRoot, expectedWithRunAsNonRoot),
        Arguments.of(contextWithSupplementalGroups, expectedWithSupplementalGroups),
        Arguments.of(contextWithFsGroup, expectedWithFsGroup),
        Arguments.of(contextWithSysctls, expectedWithSysctls),
        Arguments.of(contextWithFsGroupChangePolicy, expectedWithFsGroupChangePolicy),
        Arguments.of(contextWithSeccompProfile, expectedWithSeccompProfile),
        Arguments.of(contextWithAllFields, expectedWithAllFields),
        Arguments.of(emptyContext, expectedEmptyContext)
    );
  }

  @ParameterizedTest
  @MethodSource("provideLocalObjectReferenceInputsForList")
  void testMapLocalObjectReference(
      List<io.fabric8.kubernetes.api.model.LocalObjectReference> input,
      List<Generated.LocalObjectReference> expected) {

    Iterable<? extends Generated.LocalObjectReference> result =
        armadaMapper.mapLocalObjectReference(input);

    assertIterableEquals(expected, result);
  }

  static Stream<Arguments> provideLocalObjectReferenceInputsForList() {
    io.fabric8.kubernetes.api.model.LocalObjectReference refWithName = new io.fabric8.kubernetes.api.model.LocalObjectReference();
    refWithName.setName("image-pull-secret");

    Generated.LocalObjectReference expectedRefWithName = Generated.LocalObjectReference.newBuilder()
        .setName("image-pull-secret")
        .build();

    io.fabric8.kubernetes.api.model.LocalObjectReference emptyRef = new io.fabric8.kubernetes.api.model.LocalObjectReference();

    Generated.LocalObjectReference expectedEmptyRef = Generated.LocalObjectReference.newBuilder()
        .build();

    List<io.fabric8.kubernetes.api.model.LocalObjectReference> allRefsInput = Arrays.asList(
        refWithName, emptyRef);

    List<Generated.LocalObjectReference> allRefsExpected = Arrays.asList(expectedRefWithName,
        expectedEmptyRef);

    List<io.fabric8.kubernetes.api.model.LocalObjectReference> emptyListInput = Collections.emptyList();
    List<Generated.LocalObjectReference> emptyListExpected = Collections.emptyList();

    return Stream.of(
        Arguments.of(Collections.singletonList(refWithName),
            Collections.singletonList(expectedRefWithName)),
        Arguments.of(Collections.singletonList(emptyRef),
            Collections.singletonList(expectedEmptyRef)),
        Arguments.of(allRefsInput, allRefsExpected),
        Arguments.of(emptyListInput, emptyListExpected)
    );
  }

  @ParameterizedTest
  @MethodSource("provideNodeSelectorRequirementInputs")
  void testMapNodeSelectorRequirement(
      List<io.fabric8.kubernetes.api.model.NodeSelectorRequirement> input,
      List<Generated.NodeSelectorRequirement> expected) {

    Iterable<? extends Generated.NodeSelectorRequirement> result = armadaMapper.mapNodeSelectorRequirement(
        input);

    assertEquals(expected, result);
  }

  static Stream<Arguments> provideNodeSelectorRequirementInputs() {
    io.fabric8.kubernetes.api.model.NodeSelectorRequirement reqWithKey = new io.fabric8.kubernetes.api.model.NodeSelectorRequirement();
    reqWithKey.setKey("key1");

    io.fabric8.kubernetes.api.model.NodeSelectorRequirement reqWithOperator = new io.fabric8.kubernetes.api.model.NodeSelectorRequirement();
    reqWithOperator.setOperator("In");

    io.fabric8.kubernetes.api.model.NodeSelectorRequirement reqWithValues = new io.fabric8.kubernetes.api.model.NodeSelectorRequirement();
    reqWithValues.setValues(Arrays.asList("value1", "value2"));

    io.fabric8.kubernetes.api.model.NodeSelectorRequirement fullReq = new io.fabric8.kubernetes.api.model.NodeSelectorRequirement();
    fullReq.setKey("key2");
    fullReq.setOperator("NotIn");
    fullReq.setValues(Arrays.asList("value3", "value4"));

    Generated.NodeSelectorRequirement expectedWithKey = Generated.NodeSelectorRequirement.newBuilder()
        .setKey("key1")
        .build();

    Generated.NodeSelectorRequirement expectedWithOperator = Generated.NodeSelectorRequirement.newBuilder()
        .setOperator("In")
        .build();

    Generated.NodeSelectorRequirement expectedWithValues = Generated.NodeSelectorRequirement.newBuilder()
        .addAllValues(Arrays.asList("value1", "value2"))
        .build();

    Generated.NodeSelectorRequirement expectedFullReq = Generated.NodeSelectorRequirement.newBuilder()
        .setKey("key2")
        .setOperator("NotIn")
        .addAllValues(Arrays.asList("value3", "value4"))
        .build();

    List<io.fabric8.kubernetes.api.model.NodeSelectorRequirement> allReqsInput = Arrays.asList(
        reqWithKey, reqWithOperator, reqWithValues, fullReq);

    List<Generated.NodeSelectorRequirement> allReqsExpected = Arrays.asList(expectedWithKey,
        expectedWithOperator, expectedWithValues, expectedFullReq);

    List<io.fabric8.kubernetes.api.model.NodeSelectorRequirement> emptyListInput = Collections.emptyList();
    List<Generated.NodeSelectorRequirement> emptyListExpected = Collections.emptyList();

    return Stream.of(
        Arguments.of(Collections.singletonList(reqWithKey),
            Collections.singletonList(expectedWithKey)),
        Arguments.of(Collections.singletonList(reqWithOperator),
            Collections.singletonList(expectedWithOperator)),
        Arguments.of(Collections.singletonList(reqWithValues),
            Collections.singletonList(expectedWithValues)),
        Arguments.of(Collections.singletonList(fullReq),
            Collections.singletonList(expectedFullReq)),
        Arguments.of(allReqsInput, allReqsExpected),
        Arguments.of(emptyListInput, emptyListExpected)
    );
  }

  @ParameterizedTest
  @MethodSource("provideNodeSelectorTermsInputs")
  void testMapNodeSelectorTerms(
      List<io.fabric8.kubernetes.api.model.NodeSelectorTerm> input,
      List<Generated.NodeSelectorTerm> expected) {

    Iterable<? extends Generated.NodeSelectorTerm> result = armadaMapper.mapNodeSelectorTerms(
        input);

    assertEquals(expected, result);
  }

  static Stream<Arguments> provideNodeSelectorTermsInputs() {
    io.fabric8.kubernetes.api.model.NodeSelectorRequirement req1 = new io.fabric8.kubernetes.api.model.NodeSelectorRequirement();
    req1.setKey("key1");
    req1.setOperator("In");
    req1.setValues(Arrays.asList("value1", "value2"));

    io.fabric8.kubernetes.api.model.NodeSelectorRequirement req2 = new io.fabric8.kubernetes.api.model.NodeSelectorRequirement();
    req2.setKey("key2");
    req2.setOperator("NotIn");
    req2.setValues(List.of("value3"));

    io.fabric8.kubernetes.api.model.NodeSelectorTerm termWithMatchExpressions = new io.fabric8.kubernetes.api.model.NodeSelectorTerm();
    termWithMatchExpressions.setMatchExpressions(List.of(req1));

    io.fabric8.kubernetes.api.model.NodeSelectorTerm termWithMatchFields = new io.fabric8.kubernetes.api.model.NodeSelectorTerm();
    termWithMatchFields.setMatchFields(List.of(req2));

    Generated.NodeSelectorRequirement expectedReq1 = Generated.NodeSelectorRequirement.newBuilder()
        .setKey("key1")
        .setOperator("In")
        .addAllValues(Arrays.asList("value1", "value2"))
        .build();

    Generated.NodeSelectorRequirement expectedReq2 = Generated.NodeSelectorRequirement.newBuilder()
        .setKey("key2")
        .setOperator("NotIn")
        .addAllValues(List.of("value3"))
        .build();

    Generated.NodeSelectorTerm expectedTermWithMatchExpressions = Generated.NodeSelectorTerm.newBuilder()
        .addAllMatchExpressions(List.of(expectedReq1))
        .build();

    Generated.NodeSelectorTerm expectedTermWithMatchFields = Generated.NodeSelectorTerm.newBuilder()
        .addAllMatchFields(List.of(expectedReq2))
        .build();

    List<io.fabric8.kubernetes.api.model.NodeSelectorTerm> allTermsInput = Arrays.asList(
        termWithMatchExpressions, termWithMatchFields);

    List<Generated.NodeSelectorTerm> allTermsExpected = Arrays.asList(
        expectedTermWithMatchExpressions, expectedTermWithMatchFields);

    List<io.fabric8.kubernetes.api.model.NodeSelectorTerm> emptyListInput = Collections.emptyList();
    List<Generated.NodeSelectorTerm> emptyListExpected = Collections.emptyList();

    return Stream.of(
        Arguments.of(Collections.singletonList(termWithMatchExpressions),
            Collections.singletonList(expectedTermWithMatchExpressions)),
        Arguments.of(Collections.singletonList(termWithMatchFields),
            Collections.singletonList(expectedTermWithMatchFields)),
        Arguments.of(allTermsInput, allTermsExpected),
        Arguments.of(emptyListInput, emptyListExpected)
    );
  }

  @ParameterizedTest
  @MethodSource("provideNodeSelectorInputs")
  void testMapNodeSelector(
      io.fabric8.kubernetes.api.model.NodeSelector input,
      Generated.NodeSelector expected) {

    Generated.NodeSelector result = armadaMapper.mapNodeSelector(input);

    assertEquals(expected, result);
  }

  static Stream<Arguments> provideNodeSelectorInputs() {
    io.fabric8.kubernetes.api.model.NodeSelectorRequirement req1 = new io.fabric8.kubernetes.api.model.NodeSelectorRequirement();
    req1.setKey("key1");
    req1.setOperator("In");
    req1.setValues(Arrays.asList("value1", "value2"));

    io.fabric8.kubernetes.api.model.NodeSelectorRequirement req2 = new io.fabric8.kubernetes.api.model.NodeSelectorRequirement();
    req2.setKey("key2");
    req2.setOperator("NotIn");
    req2.setValues(List.of("value3"));

    io.fabric8.kubernetes.api.model.NodeSelectorTerm term1 = new io.fabric8.kubernetes.api.model.NodeSelectorTerm();
    term1.setMatchExpressions(List.of(req1));

    io.fabric8.kubernetes.api.model.NodeSelectorTerm term2 = new io.fabric8.kubernetes.api.model.NodeSelectorTerm();
    term2.setMatchFields(List.of(req2));

    io.fabric8.kubernetes.api.model.NodeSelector nodeSelectorWithTerms = new io.fabric8.kubernetes.api.model.NodeSelector();
    nodeSelectorWithTerms.setNodeSelectorTerms(Arrays.asList(term1, term2));

    Generated.NodeSelectorRequirement expectedReq1 = Generated.NodeSelectorRequirement.newBuilder()
        .setKey("key1")
        .setOperator("In")
        .addAllValues(Arrays.asList("value1", "value2"))
        .build();

    Generated.NodeSelectorRequirement expectedReq2 = Generated.NodeSelectorRequirement.newBuilder()
        .setKey("key2")
        .setOperator("NotIn")
        .addAllValues(List.of("value3"))
        .build();

    Generated.NodeSelectorTerm expectedTerm1 = Generated.NodeSelectorTerm.newBuilder()
        .addAllMatchExpressions(List.of(expectedReq1))
        .build();

    Generated.NodeSelectorTerm expectedTerm2 = Generated.NodeSelectorTerm.newBuilder()
        .addAllMatchFields(List.of(expectedReq2))
        .build();

    Generated.NodeSelector expectedWithTerms = Generated.NodeSelector.newBuilder()
        .addAllNodeSelectorTerms(Arrays.asList(expectedTerm1, expectedTerm2))
        .build();

    io.fabric8.kubernetes.api.model.NodeSelector emptyNodeSelector = new io.fabric8.kubernetes.api.model.NodeSelector();
    Generated.NodeSelector expectedEmpty = Generated.NodeSelector.newBuilder().build();

    return Stream.of(
        Arguments.of(nodeSelectorWithTerms, expectedWithTerms),
        Arguments.of(emptyNodeSelector, expectedEmpty)
    );
  }

  @ParameterizedTest
  @MethodSource("provideNodeSelectorTermInputs")
  void testMapNodeSelectorTerm(
      io.fabric8.kubernetes.api.model.NodeSelectorTerm input,
      Generated.NodeSelectorTerm expected) {

    Generated.NodeSelectorTerm result = armadaMapper.mapNodeSelector(input);

    assertEquals(expected, result);
  }

  static Stream<Arguments> provideNodeSelectorTermInputs() {
    io.fabric8.kubernetes.api.model.NodeSelectorRequirement matchExpression1 = new io.fabric8.kubernetes.api.model.NodeSelectorRequirement();
    matchExpression1.setKey("key1");
    matchExpression1.setOperator("In");
    matchExpression1.setValues(Arrays.asList("value1", "value2"));

    io.fabric8.kubernetes.api.model.NodeSelectorRequirement matchField1 = new io.fabric8.kubernetes.api.model.NodeSelectorRequirement();
    matchField1.setKey("key2");
    matchField1.setOperator("NotIn");
    matchField1.setValues(List.of("value3"));

    io.fabric8.kubernetes.api.model.NodeSelectorTerm termWithBoth = new io.fabric8.kubernetes.api.model.NodeSelectorTerm();
    termWithBoth.setMatchExpressions(List.of(matchExpression1));
    termWithBoth.setMatchFields(List.of(matchField1));

    io.fabric8.kubernetes.api.model.NodeSelectorTerm termWithExpressions = new io.fabric8.kubernetes.api.model.NodeSelectorTerm();
    termWithExpressions.setMatchExpressions(List.of(matchExpression1));

    io.fabric8.kubernetes.api.model.NodeSelectorTerm termWithFields = new io.fabric8.kubernetes.api.model.NodeSelectorTerm();
    termWithFields.setMatchFields(List.of(matchField1));

    Generated.NodeSelectorRequirement expectedMatchExpression1 = Generated.NodeSelectorRequirement.newBuilder()
        .setKey("key1")
        .setOperator("In")
        .addAllValues(Arrays.asList("value1", "value2"))
        .build();

    Generated.NodeSelectorRequirement expectedMatchField1 = Generated.NodeSelectorRequirement.newBuilder()
        .setKey("key2")
        .setOperator("NotIn")
        .addAllValues(List.of("value3"))
        .build();

    Generated.NodeSelectorTerm expectedWithBoth = Generated.NodeSelectorTerm.newBuilder()
        .addAllMatchExpressions(List.of(expectedMatchExpression1))
        .addAllMatchFields(List.of(expectedMatchField1))
        .build();

    Generated.NodeSelectorTerm expectedWithExpressions = Generated.NodeSelectorTerm.newBuilder()
        .addAllMatchExpressions(List.of(expectedMatchExpression1))
        .build();

    Generated.NodeSelectorTerm expectedWithFields = Generated.NodeSelectorTerm.newBuilder()
        .addAllMatchFields(List.of(expectedMatchField1))
        .build();

    io.fabric8.kubernetes.api.model.NodeSelectorTerm emptyTerm = new io.fabric8.kubernetes.api.model.NodeSelectorTerm();
    Generated.NodeSelectorTerm expectedEmpty = Generated.NodeSelectorTerm.newBuilder().build();

    return Stream.of(
        Arguments.of(termWithBoth, expectedWithBoth),
        Arguments.of(termWithExpressions, expectedWithExpressions),
        Arguments.of(termWithFields, expectedWithFields),
        Arguments.of(emptyTerm, expectedEmpty)
    );
  }

  @ParameterizedTest
  @MethodSource("providePreferredSchedulingTermInputs")
  void testMapPreferredSchedulingTerms(
      List<io.fabric8.kubernetes.api.model.PreferredSchedulingTerm> input,
      List<Generated.PreferredSchedulingTerm> expected) {

    Iterable<? extends Generated.PreferredSchedulingTerm> result = armadaMapper.mapPreferredSchedulingTerms(
        input);

    assertEquals(expected, result);
  }

  static Stream<Arguments> providePreferredSchedulingTermInputs() {
    io.fabric8.kubernetes.api.model.NodeSelectorRequirement matchExpression = new io.fabric8.kubernetes.api.model.NodeSelectorRequirement();
    matchExpression.setKey("key1");
    matchExpression.setOperator("Exists");

    io.fabric8.kubernetes.api.model.NodeSelectorTerm preference1 = new io.fabric8.kubernetes.api.model.NodeSelectorTerm();
    preference1.setMatchExpressions(List.of(matchExpression));

    io.fabric8.kubernetes.api.model.PreferredSchedulingTerm schedulingTermWithWeightAndPreference = new io.fabric8.kubernetes.api.model.PreferredSchedulingTerm();
    schedulingTermWithWeightAndPreference.setWeight(10);
    schedulingTermWithWeightAndPreference.setPreference(preference1);

    io.fabric8.kubernetes.api.model.PreferredSchedulingTerm schedulingTermWithWeightOnly = new io.fabric8.kubernetes.api.model.PreferredSchedulingTerm();
    schedulingTermWithWeightOnly.setWeight(5);

    io.fabric8.kubernetes.api.model.PreferredSchedulingTerm schedulingTermWithPreferenceOnly = new io.fabric8.kubernetes.api.model.PreferredSchedulingTerm();
    schedulingTermWithPreferenceOnly.setPreference(preference1);

    io.fabric8.kubernetes.api.model.PreferredSchedulingTerm emptyTerm = new io.fabric8.kubernetes.api.model.PreferredSchedulingTerm();

    Generated.NodeSelectorRequirement expectedMatchExpression = Generated.NodeSelectorRequirement.newBuilder()
        .setKey("key1")
        .setOperator("Exists")
        .build();

    Generated.NodeSelectorTerm expectedPreference1 = Generated.NodeSelectorTerm.newBuilder()
        .addAllMatchExpressions(List.of(expectedMatchExpression))
        .build();

    Generated.PreferredSchedulingTerm expectedWithWeightAndPreference = Generated.PreferredSchedulingTerm.newBuilder()
        .setWeight(10)
        .setPreference(expectedPreference1)
        .build();

    Generated.PreferredSchedulingTerm expectedWithWeightOnly = Generated.PreferredSchedulingTerm.newBuilder()
        .setWeight(5)
        .build();

    Generated.PreferredSchedulingTerm expectedWithPreferenceOnly = Generated.PreferredSchedulingTerm.newBuilder()
        .setPreference(expectedPreference1)
        .build();

    Generated.PreferredSchedulingTerm expectedEmpty = Generated.PreferredSchedulingTerm.newBuilder()
        .build();

    return Stream.of(
        Arguments.of(
            List.of(schedulingTermWithWeightAndPreference),
            List.of(expectedWithWeightAndPreference)
        ),
        Arguments.of(
            List.of(schedulingTermWithWeightOnly),
            List.of(expectedWithWeightOnly)
        ),
        Arguments.of(
            List.of(schedulingTermWithPreferenceOnly),
            List.of(expectedWithPreferenceOnly)
        ),
        Arguments.of(
            List.of(emptyTerm),
            List.of(expectedEmpty)
        ),
        Arguments.of(
            List.of(),
            List.of()
        )
    );
  }

  @ParameterizedTest
  @MethodSource("provideNodeAffinityInputs")
  void testMapNodeAffinity(
      io.fabric8.kubernetes.api.model.NodeAffinity input,
      Generated.NodeAffinity expected) {

    Generated.NodeAffinity result = armadaMapper.mapNodeAffinity(input);

    assertEquals(expected, result);
  }

  static Stream<Arguments> provideNodeAffinityInputs() {
    io.fabric8.kubernetes.api.model.NodeSelectorRequirement matchExpression = new io.fabric8.kubernetes.api.model.NodeSelectorRequirement();
    matchExpression.setKey("key1");
    matchExpression.setOperator("Exists");

    io.fabric8.kubernetes.api.model.NodeSelectorTerm nodeSelectorTerm = new io.fabric8.kubernetes.api.model.NodeSelectorTerm();
    nodeSelectorTerm.setMatchExpressions(List.of(matchExpression));

    io.fabric8.kubernetes.api.model.NodeSelector nodeSelector = new io.fabric8.kubernetes.api.model.NodeSelector();
    nodeSelector.setNodeSelectorTerms(List.of(nodeSelectorTerm));

    io.fabric8.kubernetes.api.model.PreferredSchedulingTerm preferredTerm = new io.fabric8.kubernetes.api.model.PreferredSchedulingTerm();
    preferredTerm.setWeight(10);
    preferredTerm.setPreference(nodeSelectorTerm);

    io.fabric8.kubernetes.api.model.NodeAffinity nodeAffinityWithRequired = new io.fabric8.kubernetes.api.model.NodeAffinity();
    nodeAffinityWithRequired.setRequiredDuringSchedulingIgnoredDuringExecution(nodeSelector);

    io.fabric8.kubernetes.api.model.NodeAffinity nodeAffinityWithPreferred = new io.fabric8.kubernetes.api.model.NodeAffinity();
    nodeAffinityWithPreferred.setPreferredDuringSchedulingIgnoredDuringExecution(
        List.of(preferredTerm));

    io.fabric8.kubernetes.api.model.NodeAffinity nodeAffinityWithBoth = new io.fabric8.kubernetes.api.model.NodeAffinity();
    nodeAffinityWithBoth.setRequiredDuringSchedulingIgnoredDuringExecution(nodeSelector);
    nodeAffinityWithBoth.setPreferredDuringSchedulingIgnoredDuringExecution(List.of(preferredTerm));

    io.fabric8.kubernetes.api.model.NodeAffinity emptyNodeAffinity = new io.fabric8.kubernetes.api.model.NodeAffinity();

    Generated.NodeSelectorRequirement expectedMatchExpression = Generated.NodeSelectorRequirement.newBuilder()
        .setKey("key1")
        .setOperator("Exists")
        .build();

    Generated.NodeSelectorTerm expectedNodeSelectorTerm = Generated.NodeSelectorTerm.newBuilder()
        .addAllMatchExpressions(List.of(expectedMatchExpression))
        .build();

    Generated.NodeSelector expectedNodeSelector = Generated.NodeSelector.newBuilder()
        .addAllNodeSelectorTerms(List.of(expectedNodeSelectorTerm))
        .build();

    Generated.PreferredSchedulingTerm expectedPreferredTerm = Generated.PreferredSchedulingTerm.newBuilder()
        .setWeight(10)
        .setPreference(expectedNodeSelectorTerm)
        .build();

    Generated.NodeAffinity expectedWithRequired = Generated.NodeAffinity.newBuilder()
        .setRequiredDuringSchedulingIgnoredDuringExecution(expectedNodeSelector)
        .build();

    Generated.NodeAffinity expectedWithPreferred = Generated.NodeAffinity.newBuilder()
        .addAllPreferredDuringSchedulingIgnoredDuringExecution(List.of(expectedPreferredTerm))
        .build();

    Generated.NodeAffinity expectedWithBoth = Generated.NodeAffinity.newBuilder()
        .setRequiredDuringSchedulingIgnoredDuringExecution(expectedNodeSelector)
        .addAllPreferredDuringSchedulingIgnoredDuringExecution(List.of(expectedPreferredTerm))
        .build();

    Generated.NodeAffinity expectedEmpty = Generated.NodeAffinity.newBuilder().build();

    return Stream.of(
        Arguments.of(nodeAffinityWithRequired, expectedWithRequired),
        Arguments.of(nodeAffinityWithPreferred, expectedWithPreferred),
        Arguments.of(nodeAffinityWithBoth, expectedWithBoth),
        Arguments.of(emptyNodeAffinity, expectedEmpty)
    );
  }

  @ParameterizedTest
  @MethodSource("provideLabelSelectorRequirementInputs")
  void testMapLabelSelectorRequirement(
      List<io.fabric8.kubernetes.api.model.LabelSelectorRequirement> input,
      List<LabelSelectorRequirement> expected) {

    Iterable<? extends LabelSelectorRequirement> result = armadaMapper.mapLabelSelectorRequirement(
        input);

    assertEquals(expected, result);
  }

  static Stream<Arguments> provideLabelSelectorRequirementInputs() {
    io.fabric8.kubernetes.api.model.LabelSelectorRequirement requirementWithKey = new io.fabric8.kubernetes.api.model.LabelSelectorRequirement();
    requirementWithKey.setKey("key1");

    io.fabric8.kubernetes.api.model.LabelSelectorRequirement requirementWithOperator = new io.fabric8.kubernetes.api.model.LabelSelectorRequirement();
    requirementWithOperator.setOperator("In");

    io.fabric8.kubernetes.api.model.LabelSelectorRequirement requirementWithValues = new io.fabric8.kubernetes.api.model.LabelSelectorRequirement();
    requirementWithValues.setValues(List.of("value1", "value2"));

    io.fabric8.kubernetes.api.model.LabelSelectorRequirement requirementWithAllFields = new io.fabric8.kubernetes.api.model.LabelSelectorRequirement();
    requirementWithAllFields.setKey("key2");
    requirementWithAllFields.setOperator("Exists");
    requirementWithAllFields.setValues(List.of("value3", "value4"));

    List<io.fabric8.kubernetes.api.model.LabelSelectorRequirement> emptyInput = List.of();

    LabelSelectorRequirement expectedWithKey = LabelSelectorRequirement.newBuilder()
        .setKey("key1")
        .build();

    LabelSelectorRequirement expectedWithOperator = LabelSelectorRequirement.newBuilder()
        .setOperator("In")
        .build();

    LabelSelectorRequirement expectedWithValues = LabelSelectorRequirement.newBuilder()
        .addAllValues(List.of("value1", "value2"))
        .build();

    LabelSelectorRequirement expectedWithAllFields = LabelSelectorRequirement.newBuilder()
        .setKey("key2")
        .setOperator("Exists")
        .addAllValues(List.of("value3", "value4"))
        .build();

    List<LabelSelectorRequirement> emptyExpected = List.of();

    return Stream.of(
        Arguments.of(List.of(requirementWithKey), List.of(expectedWithKey)),
        Arguments.of(List.of(requirementWithOperator), List.of(expectedWithOperator)),
        Arguments.of(List.of(requirementWithValues), List.of(expectedWithValues)),
        Arguments.of(List.of(requirementWithAllFields), List.of(expectedWithAllFields)),
        Arguments.of(emptyInput, emptyExpected)
    );
  }

  @ParameterizedTest
  @MethodSource("provideLabelSelectorInputs")
  void testMapLabelSelector(
      io.fabric8.kubernetes.api.model.LabelSelector input,
      LabelSelector expected) {

    LabelSelector result = armadaMapper.mapLabelSelector(input);

    assertEquals(expected, result);
  }

  static Stream<Arguments> provideLabelSelectorInputs() {
    io.fabric8.kubernetes.api.model.LabelSelector labelSelectorWithMatchLabels = new io.fabric8.kubernetes.api.model.LabelSelector();
    labelSelectorWithMatchLabels.setMatchLabels(Map.of("key1", "value1", "key2", "value2"));

    io.fabric8.kubernetes.api.model.LabelSelector labelSelectorWithMatchExpressions = new io.fabric8.kubernetes.api.model.LabelSelector();
    io.fabric8.kubernetes.api.model.LabelSelectorRequirement matchExpression = new io.fabric8.kubernetes.api.model.LabelSelectorRequirement();
    matchExpression.setKey("key3");
    matchExpression.setOperator("Exists");
    labelSelectorWithMatchExpressions.setMatchExpressions(List.of(matchExpression));

    io.fabric8.kubernetes.api.model.LabelSelector labelSelectorWithAllFields = new io.fabric8.kubernetes.api.model.LabelSelector();
    labelSelectorWithAllFields.setMatchLabels(Map.of("key4", "value4"));
    labelSelectorWithAllFields.setMatchExpressions(List.of(matchExpression));

    io.fabric8.kubernetes.api.model.LabelSelector emptyLabelSelector = new io.fabric8.kubernetes.api.model.LabelSelector();

    LabelSelector expectedWithMatchLabels = LabelSelector.newBuilder()
        .putAllMatchLabels(Map.of("key1", "value1", "key2", "value2"))
        .build();

    LabelSelectorRequirement mappedMatchExpression = LabelSelectorRequirement.newBuilder()
        .setKey("key3")
        .setOperator("Exists")
        .build();

    LabelSelector expectedWithMatchExpressions = LabelSelector.newBuilder()
        .addAllMatchExpressions(List.of(mappedMatchExpression))
        .build();

    LabelSelector expectedWithAllFields = LabelSelector.newBuilder()
        .putAllMatchLabels(Map.of("key4", "value4"))
        .addAllMatchExpressions(List.of(mappedMatchExpression))
        .build();

    LabelSelector emptyExpected = LabelSelector.newBuilder().build();

    return Stream.of(
        Arguments.of(labelSelectorWithMatchLabels, expectedWithMatchLabels),
        Arguments.of(labelSelectorWithMatchExpressions, expectedWithMatchExpressions),
        Arguments.of(labelSelectorWithAllFields, expectedWithAllFields),
        Arguments.of(emptyLabelSelector, emptyExpected)
    );
  }

  @ParameterizedTest
  @MethodSource("providePodAffinityTermInputsForList")
  void testMapPodAffinityTerm(
      List<io.fabric8.kubernetes.api.model.PodAffinityTerm> input,
      List<Generated.PodAffinityTerm> expected) {

    Iterable<? extends Generated.PodAffinityTerm> result = armadaMapper.mapPodAffinityTerm(input);

    assertEquals(expected, result);
  }

  static Stream<Arguments> providePodAffinityTermInputsForList() {
    io.fabric8.kubernetes.api.model.PodAffinityTerm termWithLabelSelector = new io.fabric8.kubernetes.api.model.PodAffinityTerm();
    termWithLabelSelector.setLabelSelector(new io.fabric8.kubernetes.api.model.LabelSelector());
    termWithLabelSelector.getLabelSelector().setMatchLabels(Map.of("key1", "value1"));

    io.fabric8.kubernetes.api.model.PodAffinityTerm termWithNamespaces = new io.fabric8.kubernetes.api.model.PodAffinityTerm();
    termWithNamespaces.setNamespaces(List.of("namespace1", "namespace2"));

    io.fabric8.kubernetes.api.model.PodAffinityTerm termWithTopologyKey = new io.fabric8.kubernetes.api.model.PodAffinityTerm();
    termWithTopologyKey.setTopologyKey("topologyKey1");

    io.fabric8.kubernetes.api.model.PodAffinityTerm termWithNamespaceSelector = new io.fabric8.kubernetes.api.model.PodAffinityTerm();
    termWithNamespaceSelector.setNamespaceSelector(
        new io.fabric8.kubernetes.api.model.LabelSelector());
    termWithNamespaceSelector.getNamespaceSelector().setMatchLabels(Map.of("nsKey", "nsValue"));

    io.fabric8.kubernetes.api.model.PodAffinityTerm termWithMatchAndMismatchKeys = new io.fabric8.kubernetes.api.model.PodAffinityTerm();
    termWithMatchAndMismatchKeys.setMatchLabelKeys(List.of("key2", "key3"));
    termWithMatchAndMismatchKeys.setMismatchLabelKeys(List.of("key4"));

    io.fabric8.kubernetes.api.model.PodAffinityTerm termWithAllFields = new io.fabric8.kubernetes.api.model.PodAffinityTerm();
    termWithAllFields.setLabelSelector(new io.fabric8.kubernetes.api.model.LabelSelector());
    termWithAllFields.getLabelSelector().setMatchLabels(Map.of("key5", "value5"));
    termWithAllFields.setNamespaces(List.of("namespace3"));
    termWithAllFields.setTopologyKey("topologyKey2");
    termWithAllFields.setNamespaceSelector(new io.fabric8.kubernetes.api.model.LabelSelector());
    termWithAllFields.getNamespaceSelector().setMatchLabels(Map.of("nsKey2", "nsValue2"));
    termWithAllFields.setMatchLabelKeys(List.of("key6"));
    termWithAllFields.setMismatchLabelKeys(List.of("key7"));

    io.fabric8.kubernetes.api.model.PodAffinityTerm emptyTerm = new io.fabric8.kubernetes.api.model.PodAffinityTerm();

    Generated.PodAffinityTerm expectedWithLabelSelector = Generated.PodAffinityTerm.newBuilder()
        .setLabelSelector(
            LabelSelector.newBuilder().putAllMatchLabels(Map.of("key1", "value1")).build())
        .build();

    Generated.PodAffinityTerm expectedWithNamespaces = Generated.PodAffinityTerm.newBuilder()
        .addAllNamespaces(List.of("namespace1", "namespace2"))
        .build();

    Generated.PodAffinityTerm expectedWithTopologyKey = Generated.PodAffinityTerm.newBuilder()
        .setTopologyKey("topologyKey1")
        .build();

    Generated.PodAffinityTerm expectedWithNamespaceSelector = Generated.PodAffinityTerm.newBuilder()
        .setNamespaceSelector(
            LabelSelector.newBuilder().putAllMatchLabels(Map.of("nsKey", "nsValue")).build())
        .build();

    Generated.PodAffinityTerm expectedWithMatchAndMismatchKeys = Generated.PodAffinityTerm.newBuilder()
        .addAllMatchLabelKeys(List.of("key2", "key3"))
        .addAllMismatchLabelKeys(List.of("key4"))
        .build();

    Generated.PodAffinityTerm expectedWithAllFields = Generated.PodAffinityTerm.newBuilder()
        .setLabelSelector(
            LabelSelector.newBuilder().putAllMatchLabels(Map.of("key5", "value5")).build())
        .addAllNamespaces(List.of("namespace3"))
        .setTopologyKey("topologyKey2")
        .setNamespaceSelector(
            LabelSelector.newBuilder().putAllMatchLabels(Map.of("nsKey2", "nsValue2")).build())
        .addAllMatchLabelKeys(List.of("key6"))
        .addAllMismatchLabelKeys(List.of("key7"))
        .build();

    Generated.PodAffinityTerm emptyExpected = Generated.PodAffinityTerm.newBuilder().build();

    return Stream.of(
        Arguments.of(List.of(termWithLabelSelector), List.of(expectedWithLabelSelector)),
        Arguments.of(List.of(termWithNamespaces), List.of(expectedWithNamespaces)),
        Arguments.of(List.of(termWithTopologyKey), List.of(expectedWithTopologyKey)),
        Arguments.of(List.of(termWithNamespaceSelector), List.of(expectedWithNamespaceSelector)),
        Arguments.of(List.of(termWithMatchAndMismatchKeys),
            List.of(expectedWithMatchAndMismatchKeys)),
        Arguments.of(List.of(termWithAllFields), List.of(expectedWithAllFields)),
        Arguments.of(List.of(emptyTerm), List.of(emptyExpected)),
        Arguments.of(List.of(), List.of())
    );
  }

  @ParameterizedTest
  @MethodSource("providePodAffinityTermInputs")
  void testMapPodAffinityTermSingleInput(
      io.fabric8.kubernetes.api.model.PodAffinityTerm input,
      Generated.PodAffinityTerm expected) {

    Generated.PodAffinityTerm result = armadaMapper.mapPodAffinityTerm(input);

    assertEquals(expected, result);
  }

  static Stream<Arguments> providePodAffinityTermInputs() {
    io.fabric8.kubernetes.api.model.PodAffinityTerm termWithLabelSelector = new io.fabric8.kubernetes.api.model.PodAffinityTerm();
    termWithLabelSelector.setLabelSelector(new io.fabric8.kubernetes.api.model.LabelSelector());
    termWithLabelSelector.getLabelSelector().setMatchLabels(Map.of("key1", "value1"));

    io.fabric8.kubernetes.api.model.PodAffinityTerm termWithNamespaces = new io.fabric8.kubernetes.api.model.PodAffinityTerm();
    termWithNamespaces.setNamespaces(List.of("namespace1", "namespace2"));

    io.fabric8.kubernetes.api.model.PodAffinityTerm termWithTopologyKey = new io.fabric8.kubernetes.api.model.PodAffinityTerm();
    termWithTopologyKey.setTopologyKey("topologyKey1");

    io.fabric8.kubernetes.api.model.PodAffinityTerm termWithNamespaceSelector = new io.fabric8.kubernetes.api.model.PodAffinityTerm();
    termWithNamespaceSelector.setNamespaceSelector(
        new io.fabric8.kubernetes.api.model.LabelSelector());
    termWithNamespaceSelector.getNamespaceSelector().setMatchLabels(Map.of("nsKey", "nsValue"));

    io.fabric8.kubernetes.api.model.PodAffinityTerm termWithMatchAndMismatchKeys = new io.fabric8.kubernetes.api.model.PodAffinityTerm();
    termWithMatchAndMismatchKeys.setMatchLabelKeys(List.of("key2", "key3"));
    termWithMatchAndMismatchKeys.setMismatchLabelKeys(List.of("key4"));

    io.fabric8.kubernetes.api.model.PodAffinityTerm termWithAllFields = new io.fabric8.kubernetes.api.model.PodAffinityTerm();
    termWithAllFields.setLabelSelector(new io.fabric8.kubernetes.api.model.LabelSelector());
    termWithAllFields.getLabelSelector().setMatchLabels(Map.of("key5", "value5"));
    termWithAllFields.setNamespaces(List.of("namespace3"));
    termWithAllFields.setTopologyKey("topologyKey2");
    termWithAllFields.setNamespaceSelector(new io.fabric8.kubernetes.api.model.LabelSelector());
    termWithAllFields.getNamespaceSelector().setMatchLabels(Map.of("nsKey2", "nsValue2"));
    termWithAllFields.setMatchLabelKeys(List.of("key6"));
    termWithAllFields.setMismatchLabelKeys(List.of("key7"));

    io.fabric8.kubernetes.api.model.PodAffinityTerm emptyTerm = new io.fabric8.kubernetes.api.model.PodAffinityTerm();

    Generated.PodAffinityTerm expectedWithLabelSelector = Generated.PodAffinityTerm.newBuilder()
        .setLabelSelector(
            LabelSelector.newBuilder().putAllMatchLabels(Map.of("key1", "value1")).build())
        .build();

    Generated.PodAffinityTerm expectedWithNamespaces = Generated.PodAffinityTerm.newBuilder()
        .addAllNamespaces(List.of("namespace1", "namespace2"))
        .build();

    Generated.PodAffinityTerm expectedWithTopologyKey = Generated.PodAffinityTerm.newBuilder()
        .setTopologyKey("topologyKey1")
        .build();

    Generated.PodAffinityTerm expectedWithNamespaceSelector = Generated.PodAffinityTerm.newBuilder()
        .setNamespaceSelector(
            LabelSelector.newBuilder().putAllMatchLabels(Map.of("nsKey", "nsValue")).build())
        .build();

    Generated.PodAffinityTerm expectedWithMatchAndMismatchKeys = Generated.PodAffinityTerm.newBuilder()
        .addAllMatchLabelKeys(List.of("key2", "key3"))
        .addAllMismatchLabelKeys(List.of("key4"))
        .build();

    Generated.PodAffinityTerm expectedWithAllFields = Generated.PodAffinityTerm.newBuilder()
        .setLabelSelector(
            LabelSelector.newBuilder().putAllMatchLabels(Map.of("key5", "value5")).build())
        .addAllNamespaces(List.of("namespace3"))
        .setTopologyKey("topologyKey2")
        .setNamespaceSelector(
            LabelSelector.newBuilder().putAllMatchLabels(Map.of("nsKey2", "nsValue2")).build())
        .addAllMatchLabelKeys(List.of("key6"))
        .addAllMismatchLabelKeys(List.of("key7"))
        .build();

    Generated.PodAffinityTerm emptyExpected = Generated.PodAffinityTerm.newBuilder().build();

    return Stream.of(
        Arguments.of(termWithLabelSelector, expectedWithLabelSelector),
        Arguments.of(termWithNamespaces, expectedWithNamespaces),
        Arguments.of(termWithTopologyKey, expectedWithTopologyKey),
        Arguments.of(termWithNamespaceSelector, expectedWithNamespaceSelector),
        Arguments.of(termWithMatchAndMismatchKeys, expectedWithMatchAndMismatchKeys),
        Arguments.of(termWithAllFields, expectedWithAllFields),
        Arguments.of(emptyTerm, emptyExpected)
    );
  }

  @ParameterizedTest
  @MethodSource("provideWeightedPodAffinityTermInputs")
  void testMapWeightedPodAffinityTerm(
      List<io.fabric8.kubernetes.api.model.WeightedPodAffinityTerm> input,
      List<Generated.WeightedPodAffinityTerm> expected) {

    Iterable<? extends Generated.WeightedPodAffinityTerm> result = armadaMapper.mapWeightedPodAffinityTerm(
        input);

    assertEquals(expected, result);
  }

  static Stream<Arguments> provideWeightedPodAffinityTermInputs() {
    io.fabric8.kubernetes.api.model.WeightedPodAffinityTerm termWithWeight = new io.fabric8.kubernetes.api.model.WeightedPodAffinityTerm();
    termWithWeight.setWeight(10);

    io.fabric8.kubernetes.api.model.PodAffinityTerm podAffinityTerm = new io.fabric8.kubernetes.api.model.PodAffinityTerm();
    podAffinityTerm.setLabelSelector(new io.fabric8.kubernetes.api.model.LabelSelector());
    podAffinityTerm.getLabelSelector().setMatchLabels(Map.of("key1", "value1"));

    io.fabric8.kubernetes.api.model.WeightedPodAffinityTerm termWithPodAffinityTerm = new io.fabric8.kubernetes.api.model.WeightedPodAffinityTerm();
    termWithPodAffinityTerm.setPodAffinityTerm(podAffinityTerm);

    io.fabric8.kubernetes.api.model.WeightedPodAffinityTerm termWithAllFields = new io.fabric8.kubernetes.api.model.WeightedPodAffinityTerm();
    termWithAllFields.setWeight(20);
    termWithAllFields.setPodAffinityTerm(podAffinityTerm);

    io.fabric8.kubernetes.api.model.WeightedPodAffinityTerm emptyTerm = new io.fabric8.kubernetes.api.model.WeightedPodAffinityTerm();

    Generated.WeightedPodAffinityTerm expectedWithWeight = Generated.WeightedPodAffinityTerm.newBuilder()
        .setWeight(10)
        .build();

    Generated.PodAffinityTerm mappedPodAffinityTerm = Generated.PodAffinityTerm.newBuilder()
        .setLabelSelector(LabelSelector.newBuilder().putAllMatchLabels(Map.of("key1", "value1"))
            .build())
        .build();

    Generated.WeightedPodAffinityTerm expectedWithPodAffinityTerm = Generated.WeightedPodAffinityTerm.newBuilder()
        .setPodAffinityTerm(mappedPodAffinityTerm)
        .build();

    Generated.WeightedPodAffinityTerm expectedWithAllFields = Generated.WeightedPodAffinityTerm.newBuilder()
        .setWeight(20)
        .setPodAffinityTerm(mappedPodAffinityTerm)
        .build();

    Generated.WeightedPodAffinityTerm emptyExpected = Generated.WeightedPodAffinityTerm.newBuilder()
        .build();

    return Stream.of(
        Arguments.of(
            List.of(termWithWeight),
            List.of(expectedWithWeight)
        ),
        Arguments.of(
            List.of(termWithPodAffinityTerm),
            List.of(expectedWithPodAffinityTerm)
        ),
        Arguments.of(
            List.of(termWithAllFields),
            List.of(expectedWithAllFields)
        ),
        Arguments.of(
            List.of(emptyTerm),
            List.of(emptyExpected)
        ),
        Arguments.of(
            List.of(termWithWeight, termWithPodAffinityTerm, termWithAllFields, emptyTerm),
            List.of(expectedWithWeight, expectedWithPodAffinityTerm, expectedWithAllFields,
                emptyExpected)
        )
    );
  }

  @ParameterizedTest
  @MethodSource("providePodAffinityInputs")
  void testMapPodAffinity(
      io.fabric8.kubernetes.api.model.PodAffinity input,
      Generated.PodAffinity expected) {

    Generated.PodAffinity result = armadaMapper.mapPodAffinity(input);

    assertEquals(expected, result);
  }

  static Stream<Arguments> providePodAffinityInputs() {
    io.fabric8.kubernetes.api.model.PodAffinityTerm requiredTerm = new io.fabric8.kubernetes.api.model.PodAffinityTerm();
    requiredTerm.setTopologyKey("topologyKey1");
    requiredTerm.setNamespaces(List.of("namespace1"));
    requiredTerm.setLabelSelector(new io.fabric8.kubernetes.api.model.LabelSelector());
    requiredTerm.getLabelSelector().setMatchLabels(Map.of("key1", "value1"));

    io.fabric8.kubernetes.api.model.WeightedPodAffinityTerm preferredTerm = new io.fabric8.kubernetes.api.model.WeightedPodAffinityTerm();
    preferredTerm.setWeight(10);
    preferredTerm.setPodAffinityTerm(requiredTerm);

    io.fabric8.kubernetes.api.model.PodAffinity emptyAffinity = new io.fabric8.kubernetes.api.model.PodAffinity();

    io.fabric8.kubernetes.api.model.PodAffinity affinityWithRequired = new io.fabric8.kubernetes.api.model.PodAffinity();
    affinityWithRequired.setRequiredDuringSchedulingIgnoredDuringExecution(List.of(requiredTerm));

    io.fabric8.kubernetes.api.model.PodAffinity affinityWithPreferred = new io.fabric8.kubernetes.api.model.PodAffinity();
    affinityWithPreferred.setPreferredDuringSchedulingIgnoredDuringExecution(
        List.of(preferredTerm));

    io.fabric8.kubernetes.api.model.PodAffinity affinityWithAllFields = new io.fabric8.kubernetes.api.model.PodAffinity();
    affinityWithAllFields.setRequiredDuringSchedulingIgnoredDuringExecution(List.of(requiredTerm));
    affinityWithAllFields.setPreferredDuringSchedulingIgnoredDuringExecution(
        List.of(preferredTerm));

    Generated.PodAffinity emptyExpected = Generated.PodAffinity.newBuilder().build();

    Generated.PodAffinity requiredExpected = Generated.PodAffinity.newBuilder()
        .addAllRequiredDuringSchedulingIgnoredDuringExecution(
            List.of(
                Generated.PodAffinityTerm.newBuilder()
                    .setTopologyKey("topologyKey1")
                    .addAllNamespaces(List.of("namespace1"))
                    .setLabelSelector(
                        LabelSelector.newBuilder()
                            .putAllMatchLabels(Map.of("key1", "value1"))
                            .build())
                    .build()))
        .build();

    Generated.PodAffinity preferredExpected = Generated.PodAffinity.newBuilder()
        .addAllPreferredDuringSchedulingIgnoredDuringExecution(
            List.of(
                Generated.WeightedPodAffinityTerm.newBuilder()
                    .setWeight(10)
                    .setPodAffinityTerm(
                        Generated.PodAffinityTerm.newBuilder()
                            .setTopologyKey("topologyKey1")
                            .addAllNamespaces(List.of("namespace1"))
                            .setLabelSelector(
                                LabelSelector.newBuilder()
                                    .putAllMatchLabels(Map.of("key1", "value1"))
                                    .build())
                            .build())
                    .build()))
        .build();

    Generated.PodAffinity allFieldsExpected = Generated.PodAffinity.newBuilder()
        .addAllRequiredDuringSchedulingIgnoredDuringExecution(
            List.of(
                Generated.PodAffinityTerm.newBuilder()
                    .setTopologyKey("topologyKey1")
                    .addAllNamespaces(List.of("namespace1"))
                    .setLabelSelector(
                        LabelSelector.newBuilder()
                            .putAllMatchLabels(Map.of("key1", "value1"))
                            .build())
                    .build()))
        .addAllPreferredDuringSchedulingIgnoredDuringExecution(
            List.of(
                Generated.WeightedPodAffinityTerm.newBuilder()
                    .setWeight(10)
                    .setPodAffinityTerm(
                        Generated.PodAffinityTerm.newBuilder()
                            .setTopologyKey("topologyKey1")
                            .addAllNamespaces(List.of("namespace1"))
                            .setLabelSelector(
                                LabelSelector.newBuilder()
                                    .putAllMatchLabels(Map.of("key1", "value1"))
                                    .build())
                            .build())
                    .build()))
        .build();

    return Stream.of(
        Arguments.of(emptyAffinity, emptyExpected),
        Arguments.of(affinityWithRequired, requiredExpected),
        Arguments.of(affinityWithPreferred, preferredExpected),
        Arguments.of(affinityWithAllFields, allFieldsExpected)
    );
  }

  @ParameterizedTest
  @MethodSource("providePodAntiAffinityInputs")
  void testMapPodAntiAffinity(
      io.fabric8.kubernetes.api.model.PodAntiAffinity input,
      Generated.PodAntiAffinity expected) {

    Generated.PodAntiAffinity result = armadaMapper.mapPodAntiAffinity(input);

    assertEquals(expected, result);
  }

  static Stream<Arguments> providePodAntiAffinityInputs() {
    io.fabric8.kubernetes.api.model.PodAffinityTerm requiredTerm = new io.fabric8.kubernetes.api.model.PodAffinityTerm();
    requiredTerm.setTopologyKey("topologyKey1");
    requiredTerm.setNamespaces(List.of("namespace1"));
    requiredTerm.setLabelSelector(new io.fabric8.kubernetes.api.model.LabelSelector());
    requiredTerm.getLabelSelector().setMatchLabels(Map.of("key1", "value1"));

    io.fabric8.kubernetes.api.model.WeightedPodAffinityTerm preferredTerm = new io.fabric8.kubernetes.api.model.WeightedPodAffinityTerm();
    preferredTerm.setWeight(5);
    preferredTerm.setPodAffinityTerm(requiredTerm);

    io.fabric8.kubernetes.api.model.PodAntiAffinity emptyAffinity = new io.fabric8.kubernetes.api.model.PodAntiAffinity();

    io.fabric8.kubernetes.api.model.PodAntiAffinity affinityWithRequired = new io.fabric8.kubernetes.api.model.PodAntiAffinity();
    affinityWithRequired.setRequiredDuringSchedulingIgnoredDuringExecution(List.of(requiredTerm));

    io.fabric8.kubernetes.api.model.PodAntiAffinity affinityWithPreferred = new io.fabric8.kubernetes.api.model.PodAntiAffinity();
    affinityWithPreferred.setPreferredDuringSchedulingIgnoredDuringExecution(
        List.of(preferredTerm));

    io.fabric8.kubernetes.api.model.PodAntiAffinity affinityWithAllFields = new io.fabric8.kubernetes.api.model.PodAntiAffinity();
    affinityWithAllFields.setRequiredDuringSchedulingIgnoredDuringExecution(List.of(requiredTerm));
    affinityWithAllFields.setPreferredDuringSchedulingIgnoredDuringExecution(
        List.of(preferredTerm));

    Generated.PodAntiAffinity emptyExpected = Generated.PodAntiAffinity.newBuilder().build();

    Generated.PodAntiAffinity requiredExpected = Generated.PodAntiAffinity.newBuilder()
        .addAllRequiredDuringSchedulingIgnoredDuringExecution(
            List.of(
                Generated.PodAffinityTerm.newBuilder()
                    .setTopologyKey("topologyKey1")
                    .addAllNamespaces(List.of("namespace1"))
                    .setLabelSelector(
                        LabelSelector.newBuilder()
                            .putAllMatchLabels(Map.of("key1", "value1"))
                            .build())
                    .build()))
        .build();

    Generated.PodAntiAffinity preferredExpected = Generated.PodAntiAffinity.newBuilder()
        .addAllPreferredDuringSchedulingIgnoredDuringExecution(
            List.of(
                Generated.WeightedPodAffinityTerm.newBuilder()
                    .setWeight(5)
                    .setPodAffinityTerm(
                        Generated.PodAffinityTerm.newBuilder()
                            .setTopologyKey("topologyKey1")
                            .addAllNamespaces(List.of("namespace1"))
                            .setLabelSelector(
                                LabelSelector.newBuilder()
                                    .putAllMatchLabels(Map.of("key1", "value1"))
                                    .build())
                            .build())
                    .build()))
        .build();

    Generated.PodAntiAffinity allFieldsExpected = Generated.PodAntiAffinity.newBuilder()
        .addAllRequiredDuringSchedulingIgnoredDuringExecution(
            List.of(
                Generated.PodAffinityTerm.newBuilder()
                    .setTopologyKey("topologyKey1")
                    .addAllNamespaces(List.of("namespace1"))
                    .setLabelSelector(
                        LabelSelector.newBuilder()
                            .putAllMatchLabels(Map.of("key1", "value1"))
                            .build())
                    .build()))
        .addAllPreferredDuringSchedulingIgnoredDuringExecution(
            List.of(
                Generated.WeightedPodAffinityTerm.newBuilder()
                    .setWeight(5)
                    .setPodAffinityTerm(
                        Generated.PodAffinityTerm.newBuilder()
                            .setTopologyKey("topologyKey1")
                            .addAllNamespaces(List.of("namespace1"))
                            .setLabelSelector(
                                LabelSelector.newBuilder()
                                    .putAllMatchLabels(Map.of("key1", "value1"))
                                    .build())
                            .build())
                    .build()))
        .build();

    return Stream.of(
        Arguments.of(emptyAffinity, emptyExpected),
        Arguments.of(affinityWithRequired, requiredExpected),
        Arguments.of(affinityWithPreferred, preferredExpected),
        Arguments.of(affinityWithAllFields, allFieldsExpected)
    );
  }

  @ParameterizedTest
  @MethodSource("provideAffinityInputs")
  void testMapAffinity(io.fabric8.kubernetes.api.model.Affinity input,
      Generated.Affinity expected) {
    Generated.Affinity result = armadaMapper.mapAffinity(input);
    assertEquals(expected, result);
  }

  static Stream<Arguments> provideAffinityInputs() {
    io.fabric8.kubernetes.api.model.NodeAffinity nodeAffinity = new io.fabric8.kubernetes.api.model.NodeAffinity();
    io.fabric8.kubernetes.api.model.PodAffinity podAffinity = new io.fabric8.kubernetes.api.model.PodAffinity();
    io.fabric8.kubernetes.api.model.PodAntiAffinity podAntiAffinity = new io.fabric8.kubernetes.api.model.PodAntiAffinity();

    io.fabric8.kubernetes.api.model.Affinity emptyAffinity = new io.fabric8.kubernetes.api.model.Affinity();
    io.fabric8.kubernetes.api.model.Affinity affinityWithNodeAffinity = new io.fabric8.kubernetes.api.model.Affinity();
    affinityWithNodeAffinity.setNodeAffinity(nodeAffinity);
    io.fabric8.kubernetes.api.model.Affinity affinityWithPodAffinity = new io.fabric8.kubernetes.api.model.Affinity();
    affinityWithPodAffinity.setPodAffinity(podAffinity);
    io.fabric8.kubernetes.api.model.Affinity affinityWithPodAntiAffinity = new io.fabric8.kubernetes.api.model.Affinity();
    affinityWithPodAntiAffinity.setPodAntiAffinity(podAntiAffinity);
    io.fabric8.kubernetes.api.model.Affinity affinityWithAllFields = new io.fabric8.kubernetes.api.model.Affinity();
    affinityWithAllFields.setNodeAffinity(nodeAffinity);
    affinityWithAllFields.setPodAffinity(podAffinity);
    affinityWithAllFields.setPodAntiAffinity(podAntiAffinity);

    Generated.Affinity emptyExpected = Generated.Affinity.newBuilder().build();
    Generated.Affinity nodeAffinityExpected = Generated.Affinity.newBuilder()
        .setNodeAffinity(Generated.NodeAffinity.newBuilder().build())
        .build();
    Generated.Affinity podAffinityExpected = Generated.Affinity.newBuilder()
        .setPodAffinity(Generated.PodAffinity.newBuilder().build())
        .build();
    Generated.Affinity podAntiAffinityExpected = Generated.Affinity.newBuilder()
        .setPodAntiAffinity(Generated.PodAntiAffinity.newBuilder().build())
        .build();
    Generated.Affinity allFieldsExpected = Generated.Affinity.newBuilder()
        .setNodeAffinity(Generated.NodeAffinity.newBuilder().build())
        .setPodAffinity(Generated.PodAffinity.newBuilder().build())
        .setPodAntiAffinity(Generated.PodAntiAffinity.newBuilder().build())
        .build();

    return Stream.of(
        Arguments.of(emptyAffinity, emptyExpected),
        Arguments.of(affinityWithNodeAffinity, nodeAffinityExpected),
        Arguments.of(affinityWithPodAffinity, podAffinityExpected),
        Arguments.of(affinityWithPodAntiAffinity, podAntiAffinityExpected),
        Arguments.of(affinityWithAllFields, allFieldsExpected)
    );
  }

  @ParameterizedTest
  @MethodSource("provideTolerationsInputs")
  void testMapTolerations(
      List<io.fabric8.kubernetes.api.model.Toleration> input,
      List<Generated.Toleration> expected) {
    Iterable<? extends Generated.Toleration> result = armadaMapper.mapTolerations(input);
    assertEquals(expected, result);
  }

  static Stream<Arguments> provideTolerationsInputs() {
    io.fabric8.kubernetes.api.model.Toleration toleration1 =
        new io.fabric8.kubernetes.api.model.Toleration();
    toleration1.setKey("key1");

    io.fabric8.kubernetes.api.model.Toleration toleration2 =
        new io.fabric8.kubernetes.api.model.Toleration();
    toleration2.setOperator("Exists");

    io.fabric8.kubernetes.api.model.Toleration toleration3 =
        new io.fabric8.kubernetes.api.model.Toleration();
    toleration3.setValue("value3");

    io.fabric8.kubernetes.api.model.Toleration toleration4 =
        new io.fabric8.kubernetes.api.model.Toleration();
    toleration4.setEffect("NoExecute");

    io.fabric8.kubernetes.api.model.Toleration toleration5 =
        new io.fabric8.kubernetes.api.model.Toleration();
    toleration5.setTolerationSeconds(7200L);

    List<io.fabric8.kubernetes.api.model.Toleration> emptyInput = List.of();
    List<io.fabric8.kubernetes.api.model.Toleration> allInputs = List.of(toleration1, toleration2,
        toleration3, toleration4, toleration5);

    List<Generated.Toleration> emptyExpected = List.of();
    List<Generated.Toleration> allExpected = List.of(
        Generated.Toleration.newBuilder()
            .setKey("key1")
            .build(),
        Generated.Toleration.newBuilder()
            .setOperator("Exists")
            .build(),
        Generated.Toleration.newBuilder()
            .setValue("value3")
            .build(),
        Generated.Toleration.newBuilder()
            .setEffect("NoExecute")
            .build(),
        Generated.Toleration.newBuilder()
            .setTolerationSeconds(7200L)
            .build()
    );

    return Stream.of(
        Arguments.of(emptyInput, emptyExpected),
        Arguments.of(List.of(toleration1),
            List.of(Generated.Toleration.newBuilder().setKey("key1").build())),
        Arguments.of(List.of(toleration2),
            List.of(Generated.Toleration.newBuilder().setOperator("Exists").build())),
        Arguments.of(List.of(toleration3),
            List.of(Generated.Toleration.newBuilder().setValue("value3").build())),
        Arguments.of(List.of(toleration4),
            List.of(Generated.Toleration.newBuilder().setEffect("NoExecute").build())),
        Arguments.of(List.of(toleration5),
            List.of(Generated.Toleration.newBuilder().setTolerationSeconds(7200L).build())),
        Arguments.of(allInputs, allExpected)
    );
  }

  @ParameterizedTest
  @MethodSource("provideHostAliasesInputs")
  void testMapHostAliases(
      List<io.fabric8.kubernetes.api.model.HostAlias> input,
      List<Generated.HostAlias> expected) {
    Iterable<? extends Generated.HostAlias> result = armadaMapper.mapHostAliases(input);
    assertEquals(expected, result);
  }

  static Stream<Arguments> provideHostAliasesInputs() {
    io.fabric8.kubernetes.api.model.HostAlias hostAlias1 = new io.fabric8.kubernetes.api.model.HostAlias();
    hostAlias1.setIp("192.168.1.1");

    io.fabric8.kubernetes.api.model.HostAlias hostAlias2 = new io.fabric8.kubernetes.api.model.HostAlias();
    hostAlias2.setHostnames(List.of("example.com", "test.com"));

    io.fabric8.kubernetes.api.model.HostAlias hostAlias3 = new io.fabric8.kubernetes.api.model.HostAlias();
    hostAlias3.setIp("10.0.0.1");
    hostAlias3.setHostnames(List.of("internal.local", "service.local"));

    List<io.fabric8.kubernetes.api.model.HostAlias> emptyInput = List.of();
    List<io.fabric8.kubernetes.api.model.HostAlias> singleIpInput = List.of(hostAlias1);
    List<io.fabric8.kubernetes.api.model.HostAlias> singleHostnamesInput = List.of(hostAlias2);
    List<io.fabric8.kubernetes.api.model.HostAlias> combinedInput = List.of(hostAlias3);
    List<io.fabric8.kubernetes.api.model.HostAlias> allInputs = List.of(hostAlias1, hostAlias2,
        hostAlias3);

    List<Generated.HostAlias> emptyExpected = List.of();
    List<Generated.HostAlias> singleIpExpected = List.of(
        Generated.HostAlias.newBuilder()
            .setIp("192.168.1.1")
            .build()
    );
    List<Generated.HostAlias> singleHostnamesExpected = List.of(
        Generated.HostAlias.newBuilder()
            .addAllHostnames(List.of("example.com", "test.com"))
            .build()
    );
    List<Generated.HostAlias> combinedExpected = List.of(
        Generated.HostAlias.newBuilder()
            .setIp("10.0.0.1")
            .addAllHostnames(List.of("internal.local", "service.local"))
            .build()
    );
    List<Generated.HostAlias> allExpected = List.of(
        Generated.HostAlias.newBuilder()
            .setIp("192.168.1.1")
            .build(),
        Generated.HostAlias.newBuilder()
            .addAllHostnames(List.of("example.com", "test.com"))
            .build(),
        Generated.HostAlias.newBuilder()
            .setIp("10.0.0.1")
            .addAllHostnames(List.of("internal.local", "service.local"))
            .build()
    );

    return Stream.of(
        Arguments.of(emptyInput, emptyExpected),
        Arguments.of(singleIpInput, singleIpExpected),
        Arguments.of(singleHostnamesInput, singleHostnamesExpected),
        Arguments.of(combinedInput, combinedExpected),
        Arguments.of(allInputs, allExpected)
    );
  }

  @ParameterizedTest
  @MethodSource("providePodDNSConfigOptionInputs")
  void testMapPodDNSConfigOption(
      List<io.fabric8.kubernetes.api.model.PodDNSConfigOption> input,
      List<Generated.PodDNSConfigOption> expected) {
    Iterable<? extends Generated.PodDNSConfigOption> result = armadaMapper.mapPodDNSConfigOption(
        input);
    assertEquals(expected, result);
  }

  static Stream<Arguments> providePodDNSConfigOptionInputs() {
    io.fabric8.kubernetes.api.model.PodDNSConfigOption option1 = new io.fabric8.kubernetes.api.model.PodDNSConfigOption();
    option1.setName("ndots");
    option1.setValue("5");

    io.fabric8.kubernetes.api.model.PodDNSConfigOption option2 = new io.fabric8.kubernetes.api.model.PodDNSConfigOption();
    option2.setName("timeout");

    io.fabric8.kubernetes.api.model.PodDNSConfigOption option3 = new io.fabric8.kubernetes.api.model.PodDNSConfigOption();
    option3.setValue("15");

    List<io.fabric8.kubernetes.api.model.PodDNSConfigOption> emptyInput = List.of();
    List<io.fabric8.kubernetes.api.model.PodDNSConfigOption> singleFullOptionInput = List.of(
        option1);
    List<io.fabric8.kubernetes.api.model.PodDNSConfigOption> singleNameOnlyInput = List.of(option2);
    List<io.fabric8.kubernetes.api.model.PodDNSConfigOption> singleValueOnlyInput = List.of(
        option3);
    List<io.fabric8.kubernetes.api.model.PodDNSConfigOption> allInputs = List.of(option1, option2,
        option3);

    List<Generated.PodDNSConfigOption> emptyExpected = List.of();
    List<Generated.PodDNSConfigOption> singleFullOptionExpected = List.of(
        Generated.PodDNSConfigOption.newBuilder()
            .setName("ndots")
            .setValue("5")
            .build()
    );
    List<Generated.PodDNSConfigOption> singleNameOnlyExpected = List.of(
        Generated.PodDNSConfigOption.newBuilder()
            .setName("timeout")
            .build()
    );
    List<Generated.PodDNSConfigOption> singleValueOnlyExpected = List.of(
        Generated.PodDNSConfigOption.newBuilder()
            .setValue("15")
            .build()
    );
    List<Generated.PodDNSConfigOption> allExpected = List.of(
        Generated.PodDNSConfigOption.newBuilder()
            .setName("ndots")
            .setValue("5")
            .build(),
        Generated.PodDNSConfigOption.newBuilder()
            .setName("timeout")
            .build(),
        Generated.PodDNSConfigOption.newBuilder()
            .setValue("15")
            .build()
    );

    return Stream.of(
        Arguments.of(emptyInput, emptyExpected),
        Arguments.of(singleFullOptionInput, singleFullOptionExpected),
        Arguments.of(singleNameOnlyInput, singleNameOnlyExpected),
        Arguments.of(singleValueOnlyInput, singleValueOnlyExpected),
        Arguments.of(allInputs, allExpected)
    );
  }

  @ParameterizedTest
  @MethodSource("providePodDnsConfigInputs")
  void testMapPodDnsConfig(
      io.fabric8.kubernetes.api.model.PodDNSConfig input,
      Generated.PodDNSConfig expected) {
    Generated.PodDNSConfig result = armadaMapper.mapPodDnsConfig(input);
    assertEquals(expected, result);
  }

  static Stream<Arguments> providePodDnsConfigInputs() {
    io.fabric8.kubernetes.api.model.PodDNSConfig emptyDnsConfig = new io.fabric8.kubernetes.api.model.PodDNSConfig();

    io.fabric8.kubernetes.api.model.PodDNSConfig nameserversOnly = new io.fabric8.kubernetes.api.model.PodDNSConfig();
    nameserversOnly.setNameservers(List.of("8.8.8.8", "8.8.4.4"));

    io.fabric8.kubernetes.api.model.PodDNSConfig searchesOnly = new io.fabric8.kubernetes.api.model.PodDNSConfig();
    searchesOnly.setSearches(List.of("example.com", "test.com"));

    io.fabric8.kubernetes.api.model.PodDNSConfig optionsOnly = new io.fabric8.kubernetes.api.model.PodDNSConfig();
    io.fabric8.kubernetes.api.model.PodDNSConfigOption option1 = new io.fabric8.kubernetes.api.model.PodDNSConfigOption();
    option1.setName("ndots");
    option1.setValue("5");
    optionsOnly.setOptions(List.of(option1));

    io.fabric8.kubernetes.api.model.PodDNSConfig fullDnsConfig = new io.fabric8.kubernetes.api.model.PodDNSConfig();
    fullDnsConfig.setNameservers(List.of("8.8.8.8", "8.8.4.4"));
    fullDnsConfig.setSearches(List.of("example.com", "test.com"));
    fullDnsConfig.setOptions(List.of(option1));

    Generated.PodDNSConfig emptyExpected = Generated.PodDNSConfig.newBuilder().build();

    Generated.PodDNSConfig nameserversOnlyExpected = Generated.PodDNSConfig.newBuilder()
        .addAllNameservers(List.of("8.8.8.8", "8.8.4.4"))
        .build();

    Generated.PodDNSConfig searchesOnlyExpected = Generated.PodDNSConfig.newBuilder()
        .addAllSearches(List.of("example.com", "test.com"))
        .build();

    Generated.PodDNSConfig optionsOnlyExpected = Generated.PodDNSConfig.newBuilder()
        .addAllOptions(
            List.of(
                Generated.PodDNSConfigOption.newBuilder()
                    .setName("ndots")
                    .setValue("5")
                    .build()
            )
        )
        .build();

    Generated.PodDNSConfig fullExpected = Generated.PodDNSConfig.newBuilder()
        .addAllNameservers(List.of("8.8.8.8", "8.8.4.4"))
        .addAllSearches(List.of("example.com", "test.com"))
        .addAllOptions(
            List.of(
                Generated.PodDNSConfigOption.newBuilder()
                    .setName("ndots")
                    .setValue("5")
                    .build()
            )
        )
        .build();

    return Stream.of(
        Arguments.of(emptyDnsConfig, emptyExpected),
        Arguments.of(nameserversOnly, nameserversOnlyExpected),
        Arguments.of(searchesOnly, searchesOnlyExpected),
        Arguments.of(optionsOnly, optionsOnlyExpected),
        Arguments.of(fullDnsConfig, fullExpected)
    );
  }

  @ParameterizedTest
  @MethodSource("providePodReadinessGateInputs")
  void testMapPodReadinessGate(
      List<io.fabric8.kubernetes.api.model.PodReadinessGate> input,
      Iterable<? extends Generated.PodReadinessGate> expected) {
    Iterable<? extends Generated.PodReadinessGate> result = armadaMapper.mapPodReadinessGate(input);
    assertEquals(expected, result);
  }

  static Stream<Arguments> providePodReadinessGateInputs() {
    io.fabric8.kubernetes.api.model.PodReadinessGate emptyGate = new io.fabric8.kubernetes.api.model.PodReadinessGate();

    io.fabric8.kubernetes.api.model.PodReadinessGate conditionTypeOnly = new io.fabric8.kubernetes.api.model.PodReadinessGate();
    conditionTypeOnly.setConditionType("ConditionA");

    io.fabric8.kubernetes.api.model.PodReadinessGate anotherConditionType = new io.fabric8.kubernetes.api.model.PodReadinessGate();
    anotherConditionType.setConditionType("ConditionB");

    io.fabric8.kubernetes.api.model.PodReadinessGate multipleGates = new io.fabric8.kubernetes.api.model.PodReadinessGate();
    multipleGates.setConditionType("ConditionA");
    io.fabric8.kubernetes.api.model.PodReadinessGate gateB = new io.fabric8.kubernetes.api.model.PodReadinessGate();
    gateB.setConditionType("ConditionB");

    Generated.PodReadinessGate emptyGateExpected = Generated.PodReadinessGate.newBuilder().build();

    Generated.PodReadinessGate conditionTypeOnlyExpected = Generated.PodReadinessGate.newBuilder()
        .setConditionType("ConditionA")
        .build();

    Generated.PodReadinessGate anotherConditionTypeExpected = Generated.PodReadinessGate.newBuilder()
        .setConditionType("ConditionB")
        .build();

    Generated.PodReadinessGate multipleGatesExpected = Generated.PodReadinessGate.newBuilder()
        .setConditionType("ConditionA")
        .build();

    Generated.PodReadinessGate multipleGatesBExpected = Generated.PodReadinessGate.newBuilder()
        .setConditionType("ConditionB")
        .build();

    return Stream.of(
        Arguments.of(List.of(emptyGate), List.of(emptyGateExpected)),
        Arguments.of(List.of(conditionTypeOnly), List.of(conditionTypeOnlyExpected)),
        Arguments.of(List.of(anotherConditionType), List.of(anotherConditionTypeExpected)),
        Arguments.of(List.of(multipleGates, gateB),
            List.of(multipleGatesExpected, multipleGatesBExpected))
    );
  }

  @ParameterizedTest
  @MethodSource("provideOverheadInputs")
  void testMapOverhead(
      Map<String, io.fabric8.kubernetes.api.model.Quantity> input,
      Map<String, Quantity> expected) {
    Map<String, Quantity> result = armadaMapper.mapOvearhead(input);
    assertEquals(expected, result);
  }

  static Stream<Arguments> provideOverheadInputs() {
    io.fabric8.kubernetes.api.model.Quantity quantity1 = new io.fabric8.kubernetes.api.model.Quantity();
    quantity1.setAmount("10");

    io.fabric8.kubernetes.api.model.Quantity quantity2 = new io.fabric8.kubernetes.api.model.Quantity();
    quantity2.setAmount("20");

    io.fabric8.kubernetes.api.model.Quantity quantity3 = new io.fabric8.kubernetes.api.model.Quantity();
    quantity3.setAmount("30");

    Map<String, io.fabric8.kubernetes.api.model.Quantity> input1 = Map.of(
        "key1", quantity1,
        "key2", quantity2
    );

    Map<String, io.fabric8.kubernetes.api.model.Quantity> input2 = Map.of(
        "key3", quantity3
    );

    Map<String, Quantity> expected1 = Map.of(
        "key1", Quantity.newBuilder().setString("10").build(),
        "key2", Quantity.newBuilder().setString("20").build()
    );

    Map<String, Quantity> expected2 = Map.of(
        "key3", Quantity.newBuilder().setString("30").build()
    );

    return Stream.of(
        Arguments.of(input1, expected1),
        Arguments.of(input2, expected2)
    );
  }

  @ParameterizedTest
  @MethodSource("provideTopologySpreadConstraintsInputs")
  void testMapTopologySpreadConstraints(
      List<io.fabric8.kubernetes.api.model.TopologySpreadConstraint> input,
      List<Generated.TopologySpreadConstraint> expected) {
    Iterable<? extends TopologySpreadConstraint> result = armadaMapper.mapTopologySpreadConstraints(
        input);
    assertEquals(expected, result);
  }

  static Stream<Arguments> provideTopologySpreadConstraintsInputs() {
    io.fabric8.kubernetes.api.model.TopologySpreadConstraint constraintWithMaxSkew = new io.fabric8.kubernetes.api.model.TopologySpreadConstraint();
    constraintWithMaxSkew.setMaxSkew(10);

    io.fabric8.kubernetes.api.model.TopologySpreadConstraint constraintWithTopologyKey = new io.fabric8.kubernetes.api.model.TopologySpreadConstraint();
    constraintWithTopologyKey.setTopologyKey("topologyKey1");

    io.fabric8.kubernetes.api.model.TopologySpreadConstraint constraintWithWhenUnsatisfiable = new io.fabric8.kubernetes.api.model.TopologySpreadConstraint();
    constraintWithWhenUnsatisfiable.setWhenUnsatisfiable("ScheduleAnyway");

    io.fabric8.kubernetes.api.model.TopologySpreadConstraint constraintWithLabelSelector = new io.fabric8.kubernetes.api.model.TopologySpreadConstraint();
    constraintWithLabelSelector.setLabelSelector(
        new io.fabric8.kubernetes.api.model.LabelSelector());

    io.fabric8.kubernetes.api.model.TopologySpreadConstraint constraintWithMinDomains = new io.fabric8.kubernetes.api.model.TopologySpreadConstraint();
    constraintWithMinDomains.setMinDomains(3);

    io.fabric8.kubernetes.api.model.TopologySpreadConstraint constraintWithNodeAffinityPolicy = new io.fabric8.kubernetes.api.model.TopologySpreadConstraint();
    constraintWithNodeAffinityPolicy.setNodeAffinityPolicy("Affinity");

    io.fabric8.kubernetes.api.model.TopologySpreadConstraint constraintWithNodeTaintsPolicy = new io.fabric8.kubernetes.api.model.TopologySpreadConstraint();
    constraintWithNodeTaintsPolicy.setNodeTaintsPolicy("Taints");

    io.fabric8.kubernetes.api.model.TopologySpreadConstraint constraintWithMatchLabelKeys = new io.fabric8.kubernetes.api.model.TopologySpreadConstraint();
    constraintWithMatchLabelKeys.setMatchLabelKeys(List.of("label1", "label2"));

    io.fabric8.kubernetes.api.model.TopologySpreadConstraint constraintWithAllFields = new io.fabric8.kubernetes.api.model.TopologySpreadConstraint();
    constraintWithAllFields.setMaxSkew(10);
    constraintWithAllFields.setTopologyKey("topologyKey1");
    constraintWithAllFields.setWhenUnsatisfiable("ScheduleAnyway");
    constraintWithAllFields.setLabelSelector(new io.fabric8.kubernetes.api.model.LabelSelector());
    constraintWithAllFields.setMinDomains(3);
    constraintWithAllFields.setNodeAffinityPolicy("Affinity");
    constraintWithAllFields.setNodeTaintsPolicy("Taints");
    constraintWithAllFields.setMatchLabelKeys(List.of("label1", "label2"));

    List<io.fabric8.kubernetes.api.model.TopologySpreadConstraint> emptyInput = List.of();
    List<io.fabric8.kubernetes.api.model.TopologySpreadConstraint> fullInput = List.of(
        constraintWithMaxSkew, constraintWithTopologyKey, constraintWithWhenUnsatisfiable,
        constraintWithLabelSelector, constraintWithMinDomains, constraintWithNodeAffinityPolicy,
        constraintWithNodeTaintsPolicy, constraintWithMatchLabelKeys, constraintWithAllFields
    );
    List<io.fabric8.kubernetes.api.model.TopologySpreadConstraint> singleConstraintInput = List.of(
        constraintWithMaxSkew);

    Generated.TopologySpreadConstraint expectedWithMaxSkew = Generated.TopologySpreadConstraint.newBuilder()
        .setMaxSkew(10)
        .build();

    Generated.TopologySpreadConstraint expectedWithTopologyKey = Generated.TopologySpreadConstraint.newBuilder()
        .setTopologyKey("topologyKey1")
        .build();

    Generated.TopologySpreadConstraint expectedWithWhenUnsatisfiable = Generated.TopologySpreadConstraint.newBuilder()
        .setWhenUnsatisfiable("ScheduleAnyway")
        .build();

    Generated.TopologySpreadConstraint expectedWithLabelSelector = Generated.TopologySpreadConstraint.newBuilder()
        .setLabelSelector(LabelSelector.newBuilder().build())
        .build();

    Generated.TopologySpreadConstraint expectedWithMinDomains = Generated.TopologySpreadConstraint.newBuilder()
        .setMinDomains(3)
        .build();

    Generated.TopologySpreadConstraint expectedWithNodeAffinityPolicy = Generated.TopologySpreadConstraint.newBuilder()
        .setNodeAffinityPolicy("Affinity")
        .build();

    Generated.TopologySpreadConstraint expectedWithNodeTaintsPolicy = Generated.TopologySpreadConstraint.newBuilder()
        .setNodeTaintsPolicy("Taints")
        .build();

    Generated.TopologySpreadConstraint expectedWithMatchLabelKeys = Generated.TopologySpreadConstraint.newBuilder()
        .addAllMatchLabelKeys(List.of("label1", "label2"))
        .build();

    Generated.TopologySpreadConstraint expectedWithAllFields = Generated.TopologySpreadConstraint.newBuilder()
        .setMaxSkew(10)
        .setTopologyKey("topologyKey1")
        .setWhenUnsatisfiable("ScheduleAnyway")
        .setLabelSelector(LabelSelector.newBuilder().build())
        .setMinDomains(3)
        .setNodeAffinityPolicy("Affinity")
        .setNodeTaintsPolicy("Taints")
        .addAllMatchLabelKeys(List.of("label1", "label2"))
        .build();

    List<Generated.TopologySpreadConstraint> emptyExpected = List.of();
    List<Generated.TopologySpreadConstraint> fullExpected = List.of(
        expectedWithMaxSkew, expectedWithTopologyKey, expectedWithWhenUnsatisfiable,
        expectedWithLabelSelector, expectedWithMinDomains, expectedWithNodeAffinityPolicy,
        expectedWithNodeTaintsPolicy, expectedWithMatchLabelKeys, expectedWithAllFields
    );

    return Stream.of(
        Arguments.of(singleConstraintInput, List.of(expectedWithMaxSkew)),
        Arguments.of(List.of(constraintWithTopologyKey), List.of(expectedWithTopologyKey)),
        Arguments.of(List.of(constraintWithWhenUnsatisfiable),
            List.of(expectedWithWhenUnsatisfiable)),
        Arguments.of(List.of(constraintWithLabelSelector), List.of(expectedWithLabelSelector)),
        Arguments.of(List.of(constraintWithMinDomains), List.of(expectedWithMinDomains)),
        Arguments.of(List.of(constraintWithNodeAffinityPolicy),
            List.of(expectedWithNodeAffinityPolicy)),
        Arguments.of(List.of(constraintWithNodeTaintsPolicy),
            List.of(expectedWithNodeTaintsPolicy)),
        Arguments.of(List.of(constraintWithMatchLabelKeys), List.of(expectedWithMatchLabelKeys)),
        Arguments.of(List.of(constraintWithAllFields), List.of(expectedWithAllFields)),
        Arguments.of(emptyInput, emptyExpected),
        Arguments.of(fullInput, fullExpected)
    );
  }

  @ParameterizedTest
  @MethodSource("providePodOsInputs")
  void testMapPodOs(
      io.fabric8.kubernetes.api.model.PodOS input,
      Generated.PodOS expected) {
    Generated.PodOS result = armadaMapper.mapPodOs(input);
    assertEquals(expected, result);
  }

  static Stream<Arguments> providePodOsInputs() {
    io.fabric8.kubernetes.api.model.PodOS osWithName = new io.fabric8.kubernetes.api.model.PodOS();
    osWithName.setName("Linux");

    io.fabric8.kubernetes.api.model.PodOS osWithDifferentName = new io.fabric8.kubernetes.api.model.PodOS();
    osWithDifferentName.setName("Windows");

    io.fabric8.kubernetes.api.model.PodOS emptyOs = new io.fabric8.kubernetes.api.model.PodOS(); // No name set

    Generated.PodOS expectedWithName = Generated.PodOS.newBuilder()
        .setName("Linux")
        .build();

    Generated.PodOS expectedWithDifferentName = Generated.PodOS.newBuilder()
        .setName("Windows")
        .build();

    Generated.PodOS expectedEmpty = Generated.PodOS.newBuilder().build();

    return Stream.of(
        Arguments.of(osWithName, expectedWithName),
        Arguments.of(osWithDifferentName, expectedWithDifferentName),
        Arguments.of(emptyOs, expectedEmpty)
    );
  }

  @ParameterizedTest
  @MethodSource("providePodSchedulingGateInputs")
  void testMapPodSchedulingGates(
      List<io.fabric8.kubernetes.api.model.PodSchedulingGate> input,
      List<Generated.PodSchedulingGate> expected) {
    Iterable<? extends PodSchedulingGate> result = armadaMapper.mapPodSchedulingGates(input);
    assertEquals(expected, result);
  }

  static Stream<Arguments> providePodSchedulingGateInputs() {
    io.fabric8.kubernetes.api.model.PodSchedulingGate gateWithName = new io.fabric8.kubernetes.api.model.PodSchedulingGate();
    gateWithName.setName("Gate1");

    io.fabric8.kubernetes.api.model.PodSchedulingGate gateWithAnotherName = new io.fabric8.kubernetes.api.model.PodSchedulingGate();
    gateWithAnotherName.setName("Gate2");

    io.fabric8.kubernetes.api.model.PodSchedulingGate emptyGate = new io.fabric8.kubernetes.api.model.PodSchedulingGate(); // No name set

    List<io.fabric8.kubernetes.api.model.PodSchedulingGate> emptyInput = List.of();

    Generated.PodSchedulingGate expectedWithName = Generated.PodSchedulingGate.newBuilder()
        .setName("Gate1")
        .build();

    Generated.PodSchedulingGate expectedWithAnotherName = Generated.PodSchedulingGate.newBuilder()
        .setName("Gate2")
        .build();

    Generated.PodSchedulingGate expectedEmpty = Generated.PodSchedulingGate.newBuilder().build();

    List<Generated.PodSchedulingGate> emptyExpected = List.of();

    return Stream.of(
        Arguments.of(List.of(gateWithName), List.of(expectedWithName)),
        Arguments.of(List.of(gateWithAnotherName), List.of(expectedWithAnotherName)),
        Arguments.of(List.of(emptyGate), List.of(expectedEmpty)),
        Arguments.of(emptyInput, emptyExpected)
    );
  }

  @ParameterizedTest
  @MethodSource("providePodResourceClaimInputs")
  void testMapPodResourceClaims(
      List<io.fabric8.kubernetes.api.model.PodResourceClaim> input,
      List<Generated.PodResourceClaim> expected) {
    Iterable<? extends PodResourceClaim> result = armadaMapper.mapPodResourceClaims(input);
    assertEquals(expected, result);
  }

  static Stream<Arguments> providePodResourceClaimInputs() {
    io.fabric8.kubernetes.api.model.PodResourceClaim claimWithName = new io.fabric8.kubernetes.api.model.PodResourceClaim();
    claimWithName.setName("Claim1");

    io.fabric8.kubernetes.api.model.PodResourceClaim claimWithAnotherName = new io.fabric8.kubernetes.api.model.PodResourceClaim();
    claimWithAnotherName.setName("Claim2");

    io.fabric8.kubernetes.api.model.PodResourceClaim claimWithAllFields = new io.fabric8.kubernetes.api.model.PodResourceClaim();
    claimWithAllFields.setName("Claim3");

    io.fabric8.kubernetes.api.model.PodResourceClaim emptyClaim = new io.fabric8.kubernetes.api.model.PodResourceClaim();

    Generated.PodResourceClaim expectedWithName = Generated.PodResourceClaim.newBuilder()
        .setName("Claim1")
        .setResourceClaimTemplateName("Claim1")
        .build();

    Generated.PodResourceClaim expectedWithAnotherName = Generated.PodResourceClaim.newBuilder()
        .setName("Claim2")
        .setResourceClaimTemplateName("Claim2")
        .build();

    Generated.PodResourceClaim expectedWithAllFields = Generated.PodResourceClaim.newBuilder()
        .setName("Claim3")
        .setResourceClaimTemplateName("Claim3")
        .build();

    Generated.PodResourceClaim expectedEmpty = Generated.PodResourceClaim.newBuilder()
        .build();

    return Stream.of(
        Arguments.of(List.of(claimWithName), List.of(expectedWithName)),
        Arguments.of(List.of(claimWithAnotherName), List.of(expectedWithAnotherName)),
        Arguments.of(List.of(claimWithAllFields), List.of(expectedWithAllFields)),
        Arguments.of(List.of(emptyClaim), List.of(expectedEmpty))
    );
  }

  @ParameterizedTest
  @MethodSource("providePodSpecInputs")
  void testMapPodSpec(io.fabric8.kubernetes.api.model.Pod input, Generated.PodSpec expected) {
    Generated.PodSpec result = armadaMapper.mapPodSpec(input);

    assertEquals(expected, result);
  }

  static Stream<Arguments> providePodSpecInputs() {
    io.fabric8.kubernetes.api.model.Pod nullPod = new io.fabric8.kubernetes.api.model.Pod();
    Generated.PodSpec nullPodSpec = Generated.PodSpec.newBuilder().build();

    io.fabric8.kubernetes.api.model.Pod podWithVolumes = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithVolumes = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithVolumes.setVolumes(List.of(new io.fabric8.kubernetes.api.model.Volume()));
    podWithVolumes.setSpec(podSpecWithVolumes);

    Generated.PodSpec expectedPodSpecWithVolumes = Generated.PodSpec.newBuilder()
        .addAllVolumes(List.of(Generated.Volume.newBuilder()
            .setVolumeSource(Generated.VolumeSource.newBuilder().build())
            .build()))
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithContainers = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithContainers = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithContainers.setContainers(List.of(new io.fabric8.kubernetes.api.model.Container()));
    podWithContainers.setSpec(podSpecWithContainers);

    Generated.PodSpec expectedPodSpecWithContainers = Generated.PodSpec.newBuilder()
        .addAllContainers(List.of(Generated.Container.newBuilder().build()))
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithRestartPolicy = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithRestartPolicy = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithRestartPolicy.setRestartPolicy("Always");
    podWithRestartPolicy.setSpec(podSpecWithRestartPolicy);

    Generated.PodSpec expectedPodSpecWithRestartPolicy = Generated.PodSpec.newBuilder()
        .setRestartPolicy("Always")
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithTerminationGracePeriodSeconds = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithTerminationGracePeriodSeconds = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithTerminationGracePeriodSeconds.setTerminationGracePeriodSeconds(30L);
    podWithTerminationGracePeriodSeconds.setSpec(podSpecWithTerminationGracePeriodSeconds);

    Generated.PodSpec expectedPodWithTerminationGracePeriodSeconds = Generated.PodSpec.newBuilder()
        .setTerminationGracePeriodSeconds(30L)
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithActiveDeadlineSeconds = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithActiveDeadlineSeconds = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithActiveDeadlineSeconds.setActiveDeadlineSeconds(60L);
    podWithActiveDeadlineSeconds.setSpec(podSpecWithActiveDeadlineSeconds);

    Generated.PodSpec expectedPodWithActiveDeadlineSeconds = Generated.PodSpec.newBuilder()
        .setActiveDeadlineSeconds(60L)
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithDnsPolicy = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithDnsPolicy = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithDnsPolicy.setDnsPolicy("ClusterFirst");
    podWithDnsPolicy.setSpec(podSpecWithDnsPolicy);

    Generated.PodSpec expectedPodWithDnsPolicy = Generated.PodSpec.newBuilder()
        .setDnsPolicy("ClusterFirst")
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithNodeSelector = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithNodeSelector = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithNodeSelector.setNodeSelector(Map.of("key", "value"));
    podWithNodeSelector.setSpec(podSpecWithNodeSelector);

    Generated.PodSpec expectedPodWithNodeSelector = Generated.PodSpec.newBuilder()
        .putAllNodeSelector(Map.of("key", "value"))
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithServiceAccount = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithServiceAccount = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithServiceAccount.setServiceAccount("serviceAccount");
    podWithServiceAccount.setSpec(podSpecWithServiceAccount);

    Generated.PodSpec expectedPodWithServiceAccount = Generated.PodSpec.newBuilder()
        .setServiceAccount("serviceAccount")
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithAutomountServiceAccountToken = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithAutomountServiceAccountToken = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithAutomountServiceAccountToken.setAutomountServiceAccountToken(false);
    podWithAutomountServiceAccountToken.setSpec(podSpecWithAutomountServiceAccountToken);

    Generated.PodSpec expectedPodWithAutomountServiceAccountToken = Generated.PodSpec.newBuilder()
        .setAutomountServiceAccountToken(false)
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithNodeName = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithNodeName = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithNodeName.setNodeName("node1");
    podWithNodeName.setSpec(podSpecWithNodeName);

    Generated.PodSpec expectedPodWithNodeName = Generated.PodSpec.newBuilder()
        .setNodeName("node1")
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithHostNetwork = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithHostNetwork = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithHostNetwork.setHostNetwork(true);
    podWithHostNetwork.setSpec(podSpecWithHostNetwork);

    Generated.PodSpec expectedPodWithHostNetwork = Generated.PodSpec.newBuilder()
        .setHostNetwork(true)
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithHostPID = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithHostPID = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithHostPID.setHostPID(true);
    podWithHostPID.setSpec(podSpecWithHostPID);

    Generated.PodSpec expectedPodWithHostPID = Generated.PodSpec.newBuilder()
        .setHostPID(true)
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithHostIPC = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithHostIPC = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithHostIPC.setHostIPC(true);
    podWithHostIPC.setSpec(podSpecWithHostIPC);

    Generated.PodSpec expectedPodWithHostIPC = Generated.PodSpec.newBuilder()
        .setHostIPC(true)
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithShareProcessNamespace = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithShareProcessNamespace = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithShareProcessNamespace.setShareProcessNamespace(true);
    podWithShareProcessNamespace.setSpec(podSpecWithShareProcessNamespace);

    Generated.PodSpec expectedPodWithShareProcessNamespace = Generated.PodSpec.newBuilder()
        .setShareProcessNamespace(true)
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithSecurityContext = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithSecurityContext = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithSecurityContext.setSecurityContext(
        new io.fabric8.kubernetes.api.model.PodSecurityContext());
    podWithSecurityContext.setSpec(podSpecWithSecurityContext);

    Generated.PodSpec expectedPodWithSecurityContext = Generated.PodSpec.newBuilder()
        .setSecurityContext(Generated.PodSecurityContext.newBuilder().build())
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithImagePullSecrets = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithImagePullSecrets = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithImagePullSecrets.setImagePullSecrets(
        List.of(new io.fabric8.kubernetes.api.model.LocalObjectReference()));
    podWithImagePullSecrets.setSpec(podSpecWithImagePullSecrets);

    Generated.PodSpec expectedPodWithImagePullSecrets = Generated.PodSpec.newBuilder()
        .addAllImagePullSecrets(List.of(Generated.LocalObjectReference.newBuilder().build()))
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithHostname = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithHostname = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithHostname.setHostname("hostname");
    podWithHostname.setSpec(podSpecWithHostname);

    Generated.PodSpec expectedPodWithHostname = Generated.PodSpec.newBuilder()
        .setHostname("hostname")
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithSubdomain = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithSubdomain = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithSubdomain.setSubdomain("subdomain");
    podWithSubdomain.setSpec(podSpecWithSubdomain);

    Generated.PodSpec expectedPodWithSubdomain = Generated.PodSpec.newBuilder()
        .setSubdomain("subdomain")
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithAffinity = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithAffinity = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithAffinity.setAffinity(new io.fabric8.kubernetes.api.model.Affinity());
    podWithAffinity.setSpec(podSpecWithAffinity);

    Generated.PodSpec expectedPodWithAffinity = Generated.PodSpec.newBuilder()
        .setAffinity(Generated.Affinity.newBuilder().build())
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithSchedulerName = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithSchedulerName = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithSchedulerName.setSchedulerName("scheduler");
    podWithSchedulerName.setSpec(podSpecWithSchedulerName);

    Generated.PodSpec expectedPodWithSchedulerName = Generated.PodSpec.newBuilder()
        .setSchedulerName("scheduler")
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithTolerations = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithTolerations = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithTolerations.setTolerations(
        List.of(new io.fabric8.kubernetes.api.model.Toleration()));
    podWithTolerations.setSpec(podSpecWithTolerations);

    Generated.PodSpec expectedPodWithTolerations = Generated.PodSpec.newBuilder()
        .addAllTolerations(List.of(Generated.Toleration.newBuilder().build()))
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithHostAliases = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithHostAliases = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithHostAliases.setHostAliases(List.of(new io.fabric8.kubernetes.api.model.HostAlias()));
    podWithHostAliases.setSpec(podSpecWithHostAliases);

    Generated.PodSpec expectedPodWithHostAliases = Generated.PodSpec.newBuilder()
        .addAllHostAliases(List.of(Generated.HostAlias.newBuilder().build()))
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithPriorityClassName = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithPriorityClassName = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithPriorityClassName.setPriorityClassName("priorityClass");
    podWithPriorityClassName.setSpec(podSpecWithPriorityClassName);

    Generated.PodSpec expectedPodWithPriorityClassName = Generated.PodSpec.newBuilder()
        .setPriorityClassName("priorityClass")
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithPriority = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithPriority = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithPriority.setPriority(10);
    podWithPriority.setSpec(podSpecWithPriority);

    Generated.PodSpec expectedPodWithPriority = Generated.PodSpec.newBuilder()
        .setPriority(10)
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithDnsConfig = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithDnsConfig = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithDnsConfig.setDnsConfig(new io.fabric8.kubernetes.api.model.PodDNSConfig());
    podWithDnsConfig.setSpec(podSpecWithDnsConfig);

    Generated.PodSpec expectedPodWithDnsConfig = Generated.PodSpec.newBuilder()
        .setDnsConfig(Generated.PodDNSConfig.newBuilder().build())
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithReadinessGates = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithReadinessGates = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithReadinessGates.setReadinessGates(
        List.of(new io.fabric8.kubernetes.api.model.PodReadinessGate()));
    podWithReadinessGates.setSpec(podSpecWithReadinessGates);

    Generated.PodSpec expectedPodWithReadinessGates = Generated.PodSpec.newBuilder()
        .addAllReadinessGates(List.of(Generated.PodReadinessGate.newBuilder().build()))
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithRuntimeClassName = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithRuntimeClassName = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithRuntimeClassName.setRuntimeClassName("runtimeClass");
    podWithRuntimeClassName.setSpec(podSpecWithRuntimeClassName);

    Generated.PodSpec expectedPodWithRuntimeClassName = Generated.PodSpec.newBuilder()
        .setRuntimeClassName("runtimeClass")
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithEnableServiceLinks = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithEnableServiceLinks = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithEnableServiceLinks.setEnableServiceLinks(true);
    podWithEnableServiceLinks.setSpec(podSpecWithEnableServiceLinks);

    Generated.PodSpec expectedPodWithEnableServiceLinks = Generated.PodSpec.newBuilder()
        .setEnableServiceLinks(true)
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithPreemptionPolicy = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithPreemptionPolicy = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithPreemptionPolicy.setPreemptionPolicy("PreemptLowerPriority");
    podWithPreemptionPolicy.setSpec(podSpecWithPreemptionPolicy);

    Generated.PodSpec expectedPodWithPreemptionPolicy = Generated.PodSpec.newBuilder()
        .setPreemptionPolicy("PreemptLowerPriority")
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithOverhead = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithOverhead = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithOverhead.setOverhead(
        Map.of("key", new io.fabric8.kubernetes.api.model.Quantity("10", "Mi")));
    podWithOverhead.setSpec(podSpecWithOverhead);

    Generated.PodSpec expectedPodWithOverhead = Generated.PodSpec.newBuilder()
        .putAllOverhead(Map.of("key", Quantity.newBuilder().setString("10Mi").build()))
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithTopologySpreadConstraints = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithTopologySpreadConstraints = new io.fabric8.kubernetes.api.model.PodSpec();
    podWithTopologySpreadConstraints.setSpec(podSpecWithTopologySpreadConstraints);

    Generated.PodSpec expectedPodWithTopologySpreadConstraints = Generated.PodSpec.newBuilder()
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithSetHostnameAsFQDN = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithSetHostnameAsFQDN = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithSetHostnameAsFQDN.setSetHostnameAsFQDN(true);
    podWithSetHostnameAsFQDN.setSpec(podSpecWithSetHostnameAsFQDN);

    Generated.PodSpec expectedPodWithSetHostnameAsFQDN = Generated.PodSpec.newBuilder()
        .setSetHostnameAsFQDN(true)
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithOs = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithOs = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithOs.setOs(new io.fabric8.kubernetes.api.model.PodOS());
    podWithOs.setSpec(podSpecWithOs);

    Generated.PodSpec expectedPodWithOs = Generated.PodSpec.newBuilder()
        .setOs(Generated.PodOS.newBuilder().build())
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithHostUsers = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithHostUsers = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithHostUsers.setHostUsers(true);
    podWithHostUsers.setSpec(podSpecWithHostUsers);

    Generated.PodSpec expectedPodWithHostUsers = Generated.PodSpec.newBuilder()
        .setHostUsers(true)
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithSchedulingGates = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithSchedulingGates = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithSchedulingGates.setSchedulingGates(
        List.of(new io.fabric8.kubernetes.api.model.PodSchedulingGate()));
    podWithSchedulingGates.setSpec(podSpecWithSchedulingGates);

    Generated.PodSpec expectedPodWithSchedulingGates = Generated.PodSpec.newBuilder()
        .addAllSchedulingGates(List.of(Generated.PodSchedulingGate.newBuilder().build()))
        .build();

    io.fabric8.kubernetes.api.model.Pod podWithResourceClaims = new io.fabric8.kubernetes.api.model.Pod();
    io.fabric8.kubernetes.api.model.PodSpec podSpecWithResourceClaims = new io.fabric8.kubernetes.api.model.PodSpec();
    podSpecWithResourceClaims.setResourceClaims(
        List.of(new io.fabric8.kubernetes.api.model.PodResourceClaim()));
    podWithResourceClaims.setSpec(podSpecWithResourceClaims);

    Generated.PodSpec expectedPodWithResourceClaims = Generated.PodSpec.newBuilder()
        .addAllResourceClaims(List.of(Generated.PodResourceClaim.newBuilder().build()))
        .build();

    return Stream.of(
        Arguments.of(podWithVolumes, expectedPodSpecWithVolumes),
        Arguments.of(nullPod, nullPodSpec),
        Arguments.of(podWithContainers, expectedPodSpecWithContainers),
        Arguments.of(podWithRestartPolicy, expectedPodSpecWithRestartPolicy),
        Arguments.of(podWithTerminationGracePeriodSeconds,
            expectedPodWithTerminationGracePeriodSeconds),
        Arguments.of(podWithActiveDeadlineSeconds, expectedPodWithActiveDeadlineSeconds),
        Arguments.of(podWithDnsPolicy, expectedPodWithDnsPolicy),
        Arguments.of(podWithNodeSelector, expectedPodWithNodeSelector),
        Arguments.of(podWithServiceAccount, expectedPodWithServiceAccount),
        Arguments.of(podWithAutomountServiceAccountToken,
            expectedPodWithAutomountServiceAccountToken),
        Arguments.of(podWithNodeName, expectedPodWithNodeName),
        Arguments.of(podWithHostNetwork, expectedPodWithHostNetwork),
        Arguments.of(podWithHostPID, expectedPodWithHostPID),
        Arguments.of(podWithHostIPC, expectedPodWithHostIPC),
        Arguments.of(podWithShareProcessNamespace, expectedPodWithShareProcessNamespace),
        Arguments.of(podWithSecurityContext, expectedPodWithSecurityContext),
        Arguments.of(podWithImagePullSecrets, expectedPodWithImagePullSecrets),
        Arguments.of(podWithHostname, expectedPodWithHostname),
        Arguments.of(podWithSubdomain, expectedPodWithSubdomain),
        Arguments.of(podWithAffinity, expectedPodWithAffinity),
        Arguments.of(podWithSchedulerName, expectedPodWithSchedulerName),
        Arguments.of(podWithTolerations, expectedPodWithTolerations),
        Arguments.of(podWithHostAliases, expectedPodWithHostAliases),
        Arguments.of(podWithPriorityClassName, expectedPodWithPriorityClassName),
        Arguments.of(podWithPriority, expectedPodWithPriority),
        Arguments.of(podWithDnsConfig, expectedPodWithDnsConfig),
        Arguments.of(podWithReadinessGates, expectedPodWithReadinessGates),
        Arguments.of(podWithRuntimeClassName, expectedPodWithRuntimeClassName),
        Arguments.of(podWithEnableServiceLinks, expectedPodWithEnableServiceLinks),
        Arguments.of(podWithPreemptionPolicy, expectedPodWithPreemptionPolicy),
        Arguments.of(podWithOverhead, expectedPodWithOverhead),
        Arguments.of(podWithTopologySpreadConstraints, expectedPodWithTopologySpreadConstraints),
        Arguments.of(podWithSetHostnameAsFQDN, expectedPodWithSetHostnameAsFQDN),
        Arguments.of(podWithOs, expectedPodWithOs),
        Arguments.of(podWithHostUsers, expectedPodWithHostUsers),
        Arguments.of(podWithSchedulingGates, expectedPodWithSchedulingGates),
        Arguments.of(podWithResourceClaims, expectedPodWithResourceClaims));
  }

  @ParameterizedTest
  @MethodSource("providePodInputsAndExpectedResults")
  void testCreateJobRequestItems(Pod inputPod, JobSubmitRequestItem expected) {
    JobSubmitRequestItem actual = armadaMapper.createJobRequestItems(inputPod);

    assertEquals(expected, actual);
  }

  static Stream<Arguments> providePodInputsAndExpectedResults() {
    Pod podWithMetadataNull = new Pod("apiVersion", "kind", null, null, null);
    JobSubmitRequestItem expectedJobSubmitRequestItemDefault = JobSubmitRequestItem.newBuilder()
        .setNamespace("example")
        .build();

    Pod podWithMetadataEmpty = new Pod("apiVersion", "kind", new ObjectMeta(), null, null);

    ObjectMeta objectMetaLabelsOnly = new ObjectMeta();
    objectMetaLabelsOnly.setLabels(Map.of("key", "value"));
    Pod podWithMetadataLabelsOnly = new Pod("apiVersion", "kind", objectMetaLabelsOnly, null, null);
    JobSubmitRequestItem expectedJobSubmitRequestItemLabels = JobSubmitRequestItem.newBuilder()
        .setNamespace("example")
        .putAllLabels(Map.of("key", "value"))
        .build();

    ObjectMeta objectMetaAnnotationsOnly = new ObjectMeta();
    objectMetaAnnotationsOnly.setAnnotations(Map.of("key", "value"));
    Pod podWithMetadataAnnotationsOnly = new Pod("apiVersion", "kind", objectMetaAnnotationsOnly,
        null, null);
    JobSubmitRequestItem expectedJobSubmitRequestItemAnnotations = JobSubmitRequestItem.newBuilder()
        .setNamespace("example")
        .putAllAnnotations(Map.of("key", "value"))
        .build();

    PodSpec podSpec = new PodSpec();
    Pod podWithPodSpecOnly = new Pod("apiVersion", "kind", null, podSpec, null);
    JobSubmitRequestItem expectedJobSubmitRequestItemPodSpec = JobSubmitRequestItem.newBuilder()
        .setNamespace("example")
        .addPodSpecs(Generated.PodSpec.newBuilder().build())
        .build();

    return Stream.of(Arguments.of(podWithMetadataNull, expectedJobSubmitRequestItemDefault),
        Arguments.of(podWithMetadataEmpty, expectedJobSubmitRequestItemDefault),
        Arguments.of(podWithMetadataLabelsOnly, expectedJobSubmitRequestItemLabels),
        Arguments.of(podWithMetadataAnnotationsOnly, expectedJobSubmitRequestItemAnnotations),
        Arguments.of(podWithPodSpecOnly, expectedJobSubmitRequestItemPodSpec));
  }

  @Test
  void testCreateJobSubmitRequest() {
    ArmadaMapper armadaMapper = new ArmadaMapper("queue", "namespace", "jobSetId", new Pod());

    JobSubmitRequest actual = armadaMapper.createJobSubmitRequest();

    JobSubmitRequest expected = JobSubmitRequest.newBuilder()
        .setQueue("queue")
        .setJobSetId("jobSetId")
        .addJobRequestItems(JobSubmitRequestItem.newBuilder()
            .setNamespace("namespace")
            .build())
        .build();

    assertEquals(expected, actual);
  }
}