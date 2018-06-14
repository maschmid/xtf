package cz.xtf.testhelpers.image;

import com.google.gson.Gson;
import cz.xtf.core.image.Image;
import cz.xtf.core.openshift.OpenShift;
import cz.xtf.core.waiting.Waiters;
import io.fabric8.openshift.api.model.ImageStreamTag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class ImageMetadata {

	/**
	 * Creates ImageStream in provided OpenShift context and pulls back ImageStreamTag with imageUrl metadata.
	 *
	 * @param imageUrl  image url to initialize new instance of this object
	 * @param openShift context for creating ImageStream and retrieving image metadata
	 * @return new instance
	 */
	public static ImageMetadata prepare(String imageUrl, OpenShift openShift) {
		return ImageMetadata.prepare(Image.from(imageUrl), openShift);
	}

	public static ImageMetadata prepare(Image image, OpenShift openShift) {
		openShift.createImageStream(image.getImageStream());

		Waiters.sleep(TimeUnit.SECONDS, 10, "Giving OpenShift instance time to download image metadata.");
		ImageStreamTag isTag = openShift.client().imageStreamTags().withName(image.getRepo() + ":" + image.getMajorTag()).get();

		return new ImageMetadata(ModelNode.fromJSONString(new Gson().toJson(isTag.getImage().getDockerImageMetadata().getAdditionalProperties())));
	}

	private final ModelNode metadata;

	private ImageMetadata(ModelNode metadata) {
		this.metadata = metadata;
	}

	/**
	 * Returns labels on Config:Labels path.
	 *
	 * @return map of labels
	 */
	public Map<String, String> labels() {
		return metadata.get("Config", "Labels").asPropertyList().stream().collect(Collectors.toMap(Property::getName, property -> property.getValue().asString()));
	}

	/**
	 * Returns default container command on Config:Cmd path
	 *
	 * @return default command
	 */
	public String command() {
		return metadata.get("Config", "Cmd").get(0).asString();
	}

	/**
	 * Returns image environments of Config:Env path
	 *
	 * @return map of environments
	 */
	public Map<String, String> envs() {
		final Map<String, String> env = new HashMap<>();

		metadata.get("Config", "Env").asList().forEach(
				node -> {
					String[] keyValue = node.asString().split("=", 2);
					env.put(keyValue[0], keyValue[1]);
				}
		);

		return Collections.unmodifiableMap(env);
	}

	/**
	 * Returns integer set of exposed ports by specified protocol (eg. tcp, udp).
	 *
	 * @return port set
	 */
	public Set<Integer> exposedPorts(String protocol) {
		final Set<Integer> result = new HashSet<>();
		final ModelNode exposedPorts = metadata.get("Config", "ExposedPorts");

		if (exposedPorts.getType() != ModelType.UNDEFINED) {
			exposedPorts.keys().forEach(
					portDef -> {
						final String[] split = portDef.split("/");
						if (StringUtils.isBlank(protocol) || split[1].equalsIgnoreCase(protocol)) {
							result.add(Integer.parseInt(split[0]));
						}
					}
			);
		}

		return result;
	}
}