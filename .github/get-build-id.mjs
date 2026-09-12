import { appendFileSync, readFileSync } from "node:fs";

// regex credits to chatgpt cause i dont fucking know how to make regexes
const prop = (name) => readFileSync("gradle.properties", "utf8").match(new RegExp(`^${name}=(.+)$`, "m"))[1].trim();
const mc = readFileSync("gradle/libs.versions.toml", "utf8").match(/^minecraft\s*=\s*"([^"]+)"/m)[1];

const prefix = `${prop("version")}+${mc}-`;
const path = `${prop("maven_group").replaceAll(".", "/")}/${prop("archives_base_name")}/maven-metadata.xml`;

const headers = {};
if (process.env.MAVEN_USERNAME) headers.Authorization = "Basic " + Buffer.from(`${process.env.MAVEN_USERNAME}:${process.env.MAVEN_PASSWORD}`).toString("base64");

let max = 0;
const res = await fetch(`https://maven.dupers.wtf/snapshots/${path}`, {
  headers
});

if (res.ok) {
  for (const [, v] of (await res.text()).matchAll(/<version>([^<]*)<\/version>/g)) {
    const n = v.startsWith(prefix) ? Number(v.slice(prefix.length)) : NaN;
    if (n > max) max = n;
  }
}

appendFileSync(process.env.GITHUB_OUTPUT, `number=${max + 1}\n`);