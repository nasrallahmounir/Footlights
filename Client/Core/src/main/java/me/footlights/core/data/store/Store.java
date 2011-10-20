/*
 * Copyright 2011 Jonathan Anderson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.footlights.core.data.store;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import me.footlights.core.data.Block;
import me.footlights.core.data.EncryptedBlock;
import me.footlights.core.data.NoSuchBlockException;


/** Stores content blocks */
public abstract class Store
{
	/** Low-level method to put a block on disk, the network, etc. */
	protected abstract void put(String name, ByteBuffer bytes)
		throws IOException;

	/** Low-level method to get a block from the disk, network, etc. */
	protected abstract ByteBuffer get(String name)
		throws IOException, NoSuchBlockException;
	
	
	/** Constructor */
	public Store(Store cache)
	{
		this.cache = cache;
		this.journal = new LinkedList<String>();
	}


	/** Store a plaintext block. */
	public final void store(Block block) throws IOException
	{
		store(block.name(), block.getBytes());
	}

	/** Store encrypted blocks. */
	public final void store(Iterable<EncryptedBlock> blocks) throws IOException
	{
		for (EncryptedBlock b : blocks)
			store(b);
	}

	/** Store an encrypted block. */
	public final void store(EncryptedBlock block) throws IOException
	{
		store(block.name(), block.ciphertext());
	}


	/** Retrieve a stored block (returns null if no such block is found) */
	public final ByteBuffer retrieve(String name) throws IOException, NoSuchBlockException
	{
		ByteBuffer buffer = null;

		if (cache != null) buffer = cache.get(name);
		if (buffer == null) buffer = get(name);

		return buffer.asReadOnlyBuffer();
	}


	/**
	 * Flush any stored blocks to disk/network, optionally blocking until all
	 * I/O is complete.
	 */
	public void flush() throws IOException
	{
		while (true)
		{
			// ensure that we are the only thread consuming the journal
			// (it's fine for other threads to append concurrently)
			synchronized(journal)
			{
				if (journal.size() == 0) break;

				String name = journal.get(0);
				
				try
				{
					ByteBuffer buffer = cache.get(name);
					put(name, buffer);
				}
				catch(NoSuchBlockException e)
				{
					throw new IOException("Cache inconsistency: block '" + name
						+ "' not in cache '" + cache + "'");
				}
	
				journal.remove(0);
			}
		}
	}


	/**
	 * This method should not block for I/O; to ensure that the block has
	 * really been written to disk, the network, etc., call {@link #flush()}.
	 */
	private final void store(String name, ByteBuffer bytes) throws IOException
	{
		if (name == null) throw new NullPointerException("Expected block name, not null");

		if (cache == null) put(name, bytes);
		else
		{
			cache.store(name, bytes);
			journal.add(name);
		}
	}


	/** Local cache */
	private Store cache;

	/** A list of blocks stored in cache */
	private List<String> journal;
}
