import org.apache.commons.io.FilenameUtils;

def vars = getVariables()

installAnsibleRequirements(vars.requirements_file)

def inventoryContent = getAnsibleInventoryContent(vars)

ansiblePlaybook(
  extraVars: vars,
  inventoryContent: inventoryContent,
  playbook: "${vars.base_path}/ansible/playbook.yml",
  credentialsId: "${vars.dest_credentials_id}",
  disableHostKeyChecking: true,
  colorized: true,
  extras: '-v',
)

/*
 * Utility methods.
 */

/*
 * Returns a relative path to a directory where a Jenkinsfile is (without trailing slash).
 * The path is relative to a workspace.
 */
def getJobBasePath() {
  def scriptPath = currentBuild.rawBuild.parent.definition.scriptPath
  def path = FilenameUtils.getPathNoEndSeparator(scriptPath)
  return path
}

/*
 * Returns a map of required parameters (from env) and job's parameters combined.
 * All keys are in lower case.
 */
def getVariables() {
  def vars = [:]

  for (entry in Config.required_params.entrySet()) {
    def key = entry.key
    def defaultValue = entry.value

    if (!env."${key}" && defaultValue == '') {
      error("${key} is not set and has no default value. Aborting job...")
    }

    vars[key.toLowerCase()] = env."${key}" ?: defaultValue
  }

  vars.putAll(mapKeysToLowerCase(params))
  def basePath = getJobBasePath()
  vars['build_number'] = BUILD_NUMBER
  vars['base_path'] = basePath;
  vars['requirements_file'] = "${vars.base_path}/ansible/requirements.yml"

  return vars
}

/*
 * Installs ansible-galaxy requirements if the corresponding file exists.
 * This step can be skipped by setting `SKIP_ANSIBLE_REQUIREMENTS` env variable.
 * If `FORCE_ANSIBLE_REQUIREMENTS` env variable is set, the installation will be
 * executed with `--force` flag.
 */
def installAnsibleRequirements(requirementsFilePath) {
  if (fileExists(requirementsFilePath) && !env.SKIP_ANSIBLE_REQUIREMENTS) {
    def shouldForce = env.FORCE_ANSIBLE_REQUIREMENTS ? "--force" : ""
    sh "ansible-galaxy install -r ${requirementsFilePath} ${shouldForce}"
  }
}

def getAnsibleInventoryContent(vars) {
  def hostVars = vars.findAll{it.key.startsWith('hv_')}
  def hostString = "[host]\n${vars.dest_host}"

  if (vars.dest_port) {
    hostString += " ansible_port=${vars.dest_port}"
  }

  if (vars.dest_user) {
    hostString += " ansible_user=${vars.dest_user}"
  }

  hostVars.each {
    hostString = hostString.concat(" ${it.key.replace("hv_", "")}=${it.value}")
  }

  return hostString
}

/*
 * Returns a new map with keys in lower case.
 */
def mapKeysToLowerCase(map) {
  def lower = [:]
  map.each {
    lower[it.key.toLowerCase()] = it.value
  }

  return lower
}
