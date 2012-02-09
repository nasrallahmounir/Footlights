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
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

import com.google.common.collect.Lists;

import me.footlights.core.crypto.Fingerprint;
import me.footlights.core.data.Block;
import me.footlights.core.data.EncryptedBlock;
import me.footlights.core.data.File;
import me.footlights.core.data.Link;
import me.footlights.core.data.NoSuchBlockException;


/** Stores content blocks */
public abstract class Store implements java.io.Flushable
{
	/** Low-level method to put a block on disk, the network, etc. */
	protected abstract void put(Fingerprint name, ByteBuffer bytes)
		throws IOException;

	/** Low-level method to get a block from the disk, network, etc. */
	protected abstract ByteBuffer get(Fingerprint name)
		throws IOException, NoSuchBlockException;


	/** Constructor */
	public Store(LocalStore cache)
	{
		this.cache = cache;
		this.journal = Lists.newLinkedList();
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
	public final ByteBuffer retrieve(Fingerprint name) throws IOException, NoSuchBlockException
	{
		if (cache != null)
			try { return cache.retrieve(name).asReadOnlyBuffer(); }
			catch (NoSuchBlockException e) {}

		return get(name).asReadOnlyBuffer();
	}

	/**
	 * Retrieve a list of {@link File} names which are known to exist in the {@link Store}.
	 *
	 * This is not guaranteed to be an exhaustive list; we only list files in the cache (if we
	 * have one), and even that isn't guaranteed to exhaustively list anything.
	 */
	public Collection<Stat> list() throws IOException
	{
		if (cache != null) return cache.list();
		else return Lists.newArrayList();
	}


	/** Retrieve a stored (and encrypted) {@link File}. */
	public File fetch(Link link) throws GeneralSecurityException, IOException
	{
		final Fingerprint name = link.fingerprint();

		EncryptedBlock header = EncryptedBlock.newBuilder()
			.setLink(link)
			.setCiphertext(retrieve(name))
			.build();

		Block plaintext = header.plaintext();

		// Retrieve the blocks that this block links to.
		List<EncryptedBlock> content = Lists.newArrayListWithCapacity(plaintext.links().size());
		for (Link l : plaintext.links())
		{
			EncryptedBlock block = EncryptedBlock.newBuilder()
				.setLink(l)
				.setCiphertext(retrieve(l.fingerprint()))
				.build();

			content.add(block);
		}

		return File.from(header, content);
	}


	/**
	 * Flush any stored blocks to disk/network, blocking until all I/O is complete.
	 */
	public void flush() throws IOException
	{
		while (true)
		{
			// Ensure that only one thread polls from the queue at a time.
			final Fingerprint name;
			synchronized(journal)
			{
				if (journal.isEmpty()) break;
				name = journal.poll();
			}

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
		}
	}


	/**
	 * If we have a cache, this method should not block for I/O. To ensure that the block has
	 * really been written to disk, the network, etc., call {@link #flush()}.
	 */
	protected final synchronized void store(Fingerprint name, ByteBuffer bytes) throws IOException
	{
		if (name == null) throw new NullPointerException("Expected block name, not null");

		if (cache == null) put(name, bytes.asReadOnlyBuffer());
		else
		{
			cache.store(name, bytes.asReadOnlyBuffer());
			synchronized(journal) { journal.add(name); }
			notifyAll();
		}
	}


	/** Local cache */
	private LocalStore cache;

	/** A list of blocks stored in cache */
	private Queue<Fingerprint> journal;
}
