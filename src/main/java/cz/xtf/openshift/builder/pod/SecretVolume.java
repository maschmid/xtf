package cz.xtf.openshift.builder.pod;

import java.util.Map;

import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeFluent;

public class SecretVolume extends Volume {
	private final String secretName;
	private final Map<String, String> items;

	public SecretVolume(String name, String secretName) {
		super(name);
		this.secretName = secretName;
		this.items = null;
	}

	// TODO: set mode optionally
	public SecretVolume(String name, String secretName, Map<String, String> items) {
		super(name);
		this.secretName = secretName;
		this.items = items;
	}

	public String getSecretName() {
		return secretName;
	}

	@Override
	protected void addVolumeParameters(VolumeBuilder builder) {

		final VolumeFluent.SecretNested<VolumeBuilder> volumeBuilderSecretNested = builder.withNewSecret()
				.withSecretName(getSecretName());

		if (items != null) {
			items.forEach((key, path) -> {
				volumeBuilderSecretNested.addNewItem()
						.withKey(key)
						.withPath(path);
			});
		}

		volumeBuilderSecretNested.endSecret();
	}
}
