declare const TW: { XSRF?: { token: string } } | undefined;

function resolveXsrfToken(): string {
  if (typeof TW !== 'undefined' && TW.XSRF?.token) return TW.XSRF.token;
  const meta = document.querySelector<HTMLMetaElement>('meta[name="x-xsrf-token"]');
  if (meta) return meta.content;
  if (typeof TW !== 'undefined') console.warn('TwxService: TW.XSRF.token is not available; CSRF requests may fail');
  return '';
}

export interface TwxServiceOptions {
  baseUrl?: string;
  xsrfToken?: string;
}

export class TwxService {
  private baseUrl: string;
  private xsrfToken?: string;

  constructor(options: TwxServiceOptions = {}) {
    this.baseUrl = options.baseUrl || '/Thingworx';
    this.xsrfToken = options.xsrfToken;
  }

  private getHeaders(): Record<string, string> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      'X-Requested-By': 'ThingWorx',
    };
    const token = this.xsrfToken ?? resolveXsrfToken();
    if (token) headers['X-XSRF-TOKEN'] = token;
    return headers;
  }

  private async request<T>(path: string, init: RequestInit): Promise<T> {
    const response = await fetch(`${this.baseUrl}${path}`, {
      ...init,
      headers: this.getHeaders(),
    });
    const contentType = response.headers.get('content-type') || '';
    const body = response.status === 204
      ? undefined
      : contentType.includes('application/json')
        ? await response.json()
        : await response.text();
    if (!response.ok) {
      const detail = typeof body === 'string' ? body : JSON.stringify(body);
      throw new Error(`ThingWorx request failed (${response.status}): ${detail}`);
    }
    return body as T;
  }

  async invokeService<T = unknown>(
    thingName: string,
    serviceName: string,
    params: Record<string, unknown> = {}
  ): Promise<T> {
    return this.request<T>(`/Things/${encodeURIComponent(thingName)}/Services/${encodeURIComponent(serviceName)}`, {
      method: 'POST',
      body: JSON.stringify(params),
    });
  }

  async invokeServiceOnThing<T = unknown>(
    thingName: string,
    serviceName: string,
    params: Record<string, unknown> = {}
  ): Promise<T> {
    return this.invokeService<T>(thingName, serviceName, params);
  }

  async getProperty<T = unknown>(
    thingName: string,
    propertyName: string
  ): Promise<T> {
    return this.request<T>(`/Things/${encodeURIComponent(thingName)}/Properties/${encodeURIComponent(propertyName)}`, {
      method: 'GET',
    });
  }

  async setProperty(
    thingName: string,
    propertyName: string,
    value: unknown
  ): Promise<void> {
    await this.request<void>(`/Things/${encodeURIComponent(thingName)}/Properties/${encodeURIComponent(propertyName)}`, {
      method: 'PUT',
      body: JSON.stringify(value),
    });
  }

  async queryEntities<T = unknown>(
    entityType: string,
    filters?: Record<string, unknown>
  ): Promise<T> {
    const params = filters || {};
    return this.invokeService<T>('GIT.Utility.Thing', 'QueryThings', params);
  }

  async ensureInitExtensionTargets(): Promise<void> {
    await this.invokeService('GIT.Utility.Thing', 'InitExtensionImportTargets', {});
  }

  async invokeServiceWithInit<T = unknown>(
    thingName: string,
    serviceName: string,
    params: Record<string, unknown> = {}
  ): Promise<T> {
    try {
      return await this.invokeService<T>(thingName, serviceName, params);
    } catch (e: any) {
      if (e.message?.includes('GitExtensionAppKey not found')) {
        await this.ensureInitExtensionTargets();
        return this.invokeService<T>(thingName, serviceName, params);
      }
      throw e;
    }
  }
}

export const twx = new TwxService();
