package io.armadaproject.jenkins.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.SubmitOuterClass.JobSubmitRequest;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.utils.Serialization;
import k8s.io.api.core.v1.Generated.PodSpec;
import k8s.io.api.core.v1.Generated.Volume;
import k8s.io.api.core.v1.Generated.VolumeSource;
import org.junit.jupiter.api.Test;

/**
 * Volume sources declared in the agent yaml must survive the fabric8 -> protobuf conversion.
 * {@link ArmadaMapper#mapVolumeSource} once handled only emptyDir and secret, so a CSI volume was
 * submitted with its name intact but no source: Armada and Kubernetes both accepted the job and the
 * container started with an empty mount path, with no error at any layer.
 */
class ArmadaMapperVolumeSourceTest {

  /** The example Jenkinsfile pod, plus the smallest possible FFS volume. */
  private static final String YAML = """
      apiVersion: v1
      kind: Pod
      spec:
        containers:
        - name: busybox
          image: busybox
          command:
          - cat
          tty: true
          volumeMounts:
          - mountPath: /mnt
            mountPropagation: HostToContainer
            name: ffs
        volumes:
        - name: ffs
          csi:
            driver: gr-ffs
            volumeAttributes:
              mountTargets: "uat"
              readWriteShares: "RtTest"
              automount: "true"
      """;

  @Test
  void csiVolumeSourceSurvivesSubmission() {
    Pod pod = Serialization.unmarshal(YAML, Pod.class);

    // Sanity: the CSI source is present in the fabric8 model the mapper is handed.
    assertEquals("gr-ffs", pod.getSpec().getVolumes().get(0).getCsi().getDriver());

    JobSubmitRequest request =
        new ArmadaMapper("example", "example", "example", pod).createJobSubmitRequest();
    PodSpec submitted = request.getJobRequestItems(0).getPodSpecs(0);

    // The mount is submitted...
    assertEquals(
        "ffs", submitted.getContainers(0).getVolumeMounts(0).getName(), "volumeMount name");

    // ...and so is the volume, by name.
    Volume volume = submitted.getVolumes(0);
    assertEquals("ffs", volume.getName(), "volume name");

    // ...and, the part that used to be dropped, so is the source itself.
    assertTrue(volume.getVolumeSource().hasCsi(), "csi volume source reached Armada");
    assertEquals("gr-ffs", volume.getVolumeSource().getCsi().getDriver());
    assertEquals(
        "uat", volume.getVolumeSource().getCsi().getVolumeAttributesMap().get("mountTargets"));
  }

  /** A yaml attribute written with no value parses as null, which protobuf maps reject outright. */
  @Test
  void csiVolumeAttributeWithNoValue() {
    Pod pod = Serialization.unmarshal("""
        apiVersion: v1
        kind: Pod
        spec:
          containers:
          - name: busybox
            image: busybox
          volumes:
          - name: ffs
            csi:
              driver: gr-ffs
              volumeAttributes:
                automount:
        """, Pod.class);

    PodSpec submitted = new ArmadaMapper("example", "example", "example", pod)
        .createJobSubmitRequest()
        .getJobRequestItems(0)
        .getPodSpecs(0);

    assertEquals(
        "", submitted.getVolumes(0).getVolumeSource().getCsi()
            .getVolumeAttributesMap().get("automount"));
  }

  /** Every other source type the mapper claims to support. */
  private static final String OTHER_TYPES_YAML = """
      apiVersion: v1
      kind: Pod
      spec:
        containers:
        - name: busybox
          image: busybox
        volumes:
        - name: cm
          configMap:
            name: my-config
            defaultMode: 420
            items:
            - key: krb5.conf
              path: krb5.conf
        - name: hp
          hostPath:
            path: /var/lib/docker
            type: Directory
        - name: nfs
          nfs:
            server: nfs.example.com
            path: /exports
            readOnly: true
        - name: pvc
          persistentVolumeClaim:
            claimName: my-claim
            readOnly: true
        - name: git
          gitRepo:
            repository: https://example.com/repo.git
            revision: main
            directory: checkout
        - name: ebs
          awsElasticBlockStore:
            volumeID: vol-123
            fsType: ext4
            partition: 1
        - name: gce
          gcePersistentDisk:
            pdName: pd-123
            fsType: ext4
        - name: iscsi
          iscsi:
            targetPortal: 10.0.0.1:3260
            iqn: iqn.2001-04.com.example:storage
            lun: 0
            portals:
            - 10.0.0.2:3260
        - name: rbd
          rbd:
            image: rbd-image
            pool: rbd-pool
            user: admin
        - name: gluster
          glusterfs:
            endpoints: gluster-cluster
            path: gv0
            readOnly: true
        - name: flex
          flexVolume:
            driver: example.com/flex
            fsType: ext4
            options:
              shares: RtTest
      """;

  @Test
  void otherVolumeSourcesSurviveSubmission() {
    Pod pod = Serialization.unmarshal(OTHER_TYPES_YAML, Pod.class);

    PodSpec submitted = new ArmadaMapper("example", "example", "example", pod)
        .createJobSubmitRequest()
        .getJobRequestItems(0)
        .getPodSpecs(0);

    assertEquals("my-config",
        source(submitted, "cm").getConfigMap().getLocalObjectReference().getName());
    assertEquals(420, source(submitted, "cm").getConfigMap().getDefaultMode());
    assertEquals("krb5.conf", source(submitted, "cm").getConfigMap().getItems(0).getKey());

    assertEquals("/var/lib/docker", source(submitted, "hp").getHostPath().getPath());
    assertEquals("Directory", source(submitted, "hp").getHostPath().getType());

    assertEquals("nfs.example.com", source(submitted, "nfs").getNfs().getServer());
    assertEquals("/exports", source(submitted, "nfs").getNfs().getPath());
    assertTrue(source(submitted, "nfs").getNfs().getReadOnly());

    assertEquals("my-claim", source(submitted, "pvc").getPersistentVolumeClaim().getClaimName());
    assertTrue(source(submitted, "pvc").getPersistentVolumeClaim().getReadOnly());

    assertEquals("https://example.com/repo.git", source(submitted, "git").getGitRepo()
        .getRepository());
    assertEquals("checkout", source(submitted, "git").getGitRepo().getDirectory());

    assertEquals("vol-123", source(submitted, "ebs").getAwsElasticBlockStore().getVolumeID());
    assertEquals(1, source(submitted, "ebs").getAwsElasticBlockStore().getPartition());

    assertEquals("pd-123", source(submitted, "gce").getGcePersistentDisk().getPdName());

    assertEquals("10.0.0.1:3260", source(submitted, "iscsi").getIscsi().getTargetPortal());
    assertEquals("10.0.0.2:3260", source(submitted, "iscsi").getIscsi().getPortals(0));

    assertEquals("rbd-image", source(submitted, "rbd").getRbd().getImage());
    assertEquals("rbd-pool", source(submitted, "rbd").getRbd().getPool());

    assertEquals("gluster-cluster", source(submitted, "gluster").getGlusterfs().getEndpoints());
    assertTrue(source(submitted, "gluster").getGlusterfs().getReadOnly());

    assertEquals("example.com/flex", source(submitted, "flex").getFlexVolume().getDriver());
    assertEquals("RtTest",
        source(submitted, "flex").getFlexVolume().getOptionsMap().get("shares"));
  }

  /**
   * A source the mapper cannot translate must fail the submission. Submitting it would produce a
   * volume with no source, which Armada and Kubernetes accept and mount as an empty directory.
   */
  @Test
  void unmappableVolumeSourceFailsSubmission() {
    Pod pod = Serialization.unmarshal("""
        apiVersion: v1
        kind: Pod
        spec:
          containers:
          - name: busybox
            image: busybox
          volumes:
          - name: token
            projected:
              sources:
              - serviceAccountToken:
                  path: token
        """, Pod.class);

    ArmadaMapper mapper = new ArmadaMapper("example", "example", "example", pod);

    IllegalArgumentException thrown =
        assertThrows(IllegalArgumentException.class, mapper::createJobSubmitRequest);
    assertTrue(thrown.getMessage().contains("token"), thrown.getMessage());
  }

  private static VolumeSource source(PodSpec podSpec, String volumeName) {
    return podSpec.getVolumesList().stream()
        .filter(v -> volumeName.equals(v.getName()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no volume named " + volumeName))
        .getVolumeSource();
  }
}
