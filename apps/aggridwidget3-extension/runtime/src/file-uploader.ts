import JSZip from "jszip";
import { writeWidgetLog } from "./widget-logging";

export class FileUploader {
  loggingEnabled = true;
  head = "data:image/png;base64,";
  LOCAL_HOST = "/Thingworx/Things/";
  REPO_HOST = "/Thingworx/FileRepositories/";
  SAVE_SERVICE = "/Services/SaveBinary";
  EXTRACT_ZIP_SERVICE = "/Services/ExtractZipArchive";
  DELETE_FILE_SERVICE = "/Services/DeleteFile";
  HEADERS = new Headers();
  ERROR_CODES: Record<number, string> = {
    401: "You do not have access to upload files in the destination: ",
    404: "The destination of your file upload cannot be found: ",
  };
  params: any;
  uploadThumbnail = false;
  saveUrl = "";
  unzipUrl = "";
  deleteZipUrl = "";
  newFilePath = "";
  newFullPath?: string;
  container?: HTMLElement;

  log(prefix: string, message: string): void {
    writeWidgetLog(
      this.loggingEnabled,
      "AgGridWidget.FileUploader()",
      prefix,
      message,
    );
  }

  async startUpload(
    e: { target: { files: FileList } } | undefined,
    params: any,
    now: string | number,
  ): Promise<string | undefined> {
    const prefix = `.startUpload(${now}): `;
    this.log(prefix, "Starting...");
    this.params = params;
    this.loggingEnabled = params.context.EnableConsoleLogging;
    const repository = params.repository;
    this.uploadThumbnail = params.uploadThumbnail;
    this.saveUrl = this.LOCAL_HOST + repository + this.SAVE_SERVICE;
    this.unzipUrl = this.LOCAL_HOST + repository + this.EXTRACT_ZIP_SERVICE;
    this.deleteZipUrl = this.LOCAL_HOST + repository + this.DELETE_FILE_SERVICE;
    this.log(prefix, `saveUrl ${this.saveUrl}`);
    this.log(prefix, `unzipUrl ${this.unzipUrl}`);
    this.log(prefix, `deleteZipUrl ${this.deleteZipUrl}`);
    this.HEADERS.append("Content-Type", "application/json");
    this.HEADERS.append("Accept", "application/json");
    if (!e) return this.newFullPath;

    this.log(prefix, "uploaded file...");
    const file = e.target.files[0];
    try {
      const path = await this.uploadFile(file, params, now);
      this.log(prefix, `${path}, Promise resolved successfully`);
      this.newFullPath = this.REPO_HOST + repository + path;
      this.log(prefix, `this.newFullPath now = ${this.newFullPath}`);
    } catch (error) {
      this.log(prefix, `) Promise rejected, err = ${error}`);
      let message =
        "The upload attempt failed, please contact IT support and give them the following ";
      const code = Number(error);
      message += this.ERROR_CODES[code] ?? String(error);
      alert(message);
    }
    if (this.uploadThumbnail) {
      void this.uploadImageThumbnail(file, now).then(
        (result) =>
          this.log(prefix, `${result} thumbnail promise resolved successfully`),
        (error) =>
          this.log(prefix, `thumbnail promise rejected, reject = ${error}`),
      );
    }
    return this.newFullPath;
  }

  async uploadFile(
    file: File,
    params: any,
    now: string | number,
  ): Promise<string> {
    const prefix = `.uploadFile(${now}): `;
    this.log(prefix, "Starting...");
    const zip = new JSZip();
    zip.file(file.name, file);
    this.newFilePath = params.newFilePath;
    try {
      const content = await zip.generateAsync({ type: "base64" });
      const body = {
        path: `${params.newFilePath}/${now}/${file.name}.main.zip`,
        content,
        unzippedPath: `${params.newFilePath}/${now}/${file.name}`,
      };
      this.log(prefix, `body path = ${body.path}`);
      this.log(prefix, `body unzippedPath = ${body.unzippedPath}`);
      const unzipBody = {
        zipFileName: body.path,
        path: `${params.newFilePath}/${now}`,
      };
      this.log(prefix, `unzipBody = ${JSON.stringify(unzipBody)}`);
      this.log(prefix, "uploading file...");
      const saved = await fetch(this.saveUrl, {
        method: "POST",
        headers: this.HEADERS,
        body: JSON.stringify(body),
      });
      if (!saved.ok) throw new Error(String(saved.status));
      this.log(prefix, "unzipping file...");
      const unzipped = await fetch(this.unzipUrl, {
        method: "POST",
        headers: this.HEADERS,
        body: JSON.stringify(unzipBody),
      });
      if (!unzipped.ok) throw new Error(String(unzipped.status));
      await fetch(this.deleteZipUrl, {
        method: "POST",
        headers: this.HEADERS,
        body: JSON.stringify(body),
      });
      return body.unzippedPath;
    } catch (error) {
      throw new Error(String(error));
    }
  }

  async uploadImageThumbnail(
    file: File,
    now: string | number,
  ): Promise<string> {
    const prefix = ".uploadImageThumbnail(): ";
    this.log(prefix, "Starting...");
    try {
      const blob = await this.getResizedImageBlob(file);
      const zip = new JSZip();
      this.log(prefix, "created zip file...");
      zip.file(file.name, blob as Blob);
      this.log(prefix, "added canvas blob to zip file...");
      const content = await zip.generateAsync({ type: "base64" });
      this.log(prefix, "generateAsync...");
      const body = {
        path: `${this.newFilePath}/${now}/Thumbnail/${file.name}.thumbnail.zip`,
        content,
        unzippedPath: `${this.newFilePath}/${now}/Thumbnail/${file.name}`,
      };
      const unzipBody = {
        zipFileName: body.path,
        path: `${this.newFilePath}/${now}/Thumbnail`,
      };
      this.log(prefix, `uploading thumbnail file, saveUrl = ${this.saveUrl}`);
      this.log(
        prefix,
        `uploading thumbnail file, thumbnailBody = ${JSON.stringify(body)}`,
      );
      this.log(
        prefix,
        `uploading thumbnail file, unzipBody = ${JSON.stringify(unzipBody)}`,
      );
      const saved = await fetch(this.saveUrl, {
        method: "POST",
        headers: this.HEADERS,
        body: JSON.stringify(body),
      });
      if (!saved.ok) throw new Error(String(saved.status));
      this.log(prefix, "unzipping thumbnail file");
      const unzipped = await fetch(this.unzipUrl, {
        method: "POST",
        headers: this.HEADERS,
        body: JSON.stringify(unzipBody),
      });
      if (!unzipped.ok) throw new Error(String(unzipped.status));
      this.log(prefix, "deleting zipped thumbnail file");
      await fetch(this.deleteZipUrl, {
        method: "POST",
        headers: this.HEADERS,
        body: JSON.stringify(body),
      });
      return body.unzippedPath;
    } catch (error) {
      this.log(prefix, `Unzip thumbnail failed with JSON error ${error}`);
      throw new Error(String(error));
    }
  }

  async getResizedImageBlob(file: File): Promise<Blob | null> {
    const prefix = ".getResizedImageBlob(): ";
    const image = await createImageBitmap(file);
    this.log(prefix, "created img...");
    this.log(
      prefix,
      `img loaded, img.width/img.height = ${image.width}/${image.height}`,
    );
    let width = image.width;
    let height = image.height;
    if (width > height && width > 300) {
      height *= 300 / width;
      width = 300;
    } else if (height >= width && height > 300) {
      width *= 300 / height;
      height = 300;
    }
    this.log(prefix, `creating canvas, new width/height = ${width}/${height}`);
    const canvas = document.createElement("canvas");
    canvas.width = width;
    canvas.height = height;
    canvas.getContext("2d")!.drawImage(image, 0, 0, width, height);
    this.log(prefix, "drew image...");
    const blob = await new Promise<Blob | null>((resolve) =>
      canvas.toBlob(resolve, file.type, 0.3),
    );
    this.log(prefix, "returning blob...");
    return blob;
  }

  getGui(): HTMLElement | undefined {
    return this.container;
  }
  afterGuiAttached(): void {
    this.container?.focus();
  }
  getValue(): string | undefined {
    return this.newFullPath;
  }
  destroy(): void {}
  isPopup(): boolean {
    return false;
  }
}

// Preserve the recovered module's export until its callers are renamed.
